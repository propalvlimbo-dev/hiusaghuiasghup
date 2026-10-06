import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowDown,
  ArrowDownToLine,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Camera,
  ArrowUpFromLine,
  Bot,
  Check,
  CheckCircle2,
  ChevronDown,
  CircleHelp,
  Clock3,
  Command,
  ExternalLink,
  LoaderCircle,
  LockKeyhole,
  LogOut,
  Plus,
  Radio,
  RefreshCw,
  RotateCcw,
  Search,
  Server,
  Settings2,
  ShieldCheck,
  Signal,
  Wifi,
  WifiOff,
  X,
  Zap,
} from "lucide-react";
import type {
  BotRow,
  FleetAction,
  FleetOperation,
  InstanceSummary,
  SessionInfo,
} from "../shared/types";
import { api } from "./api";

const REFRESH_INTERVAL_MS = 5_000;

export default function App() {
  const [session, setSession] = useState<SessionInfo | null>(null);
  const [instances, setInstances] = useState<InstanceSummary[]>([]);
  const [operations, setOperations] = useState<FleetOperation[]>([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeView, setActiveView] = useState<"servers" | "activity">("servers");
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<"all" | "active" | "idle">("all");
  const [connectionOpen, setConnectionOpen] = useState(false);
  const [newGroupOpen, setNewGroupOpen] = useState(false);
  const [detailsGroup, setDetailsGroup] = useState<InstanceSummary | null>(null);
  const [controlBot, setControlBot] = useState<{ instance: InstanceSummary; bot: BotRow } | null>(null);
  const [confirmAction, setConfirmAction] = useState<{
    action: FleetAction;
    targets: InstanceSummary[];
  } | null>(null);
  const [toast, setToast] = useState<{ kind: "success" | "error"; message: string } | null>(null);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const toastTimer = useRef<number | null>(null);
  const refreshInFlight = useRef(false);

  const refreshData = useCallback(async (showSpinner = false) => {
    if (refreshInFlight.current) return;
    refreshInFlight.current = true;
    if (showSpinner) setRefreshing(true);
    try {
      const nextSession = await api.session();
      setSession(nextSession);
      if (nextSession.connected) {
        const [nextInstances, nextOperations] = await Promise.all([
          api.instances(),
          api.operations(),
        ]);
        setInstances(nextInstances);
        setOperations(nextOperations);
      } else {
        setInstances([]);
        setOperations(await api.operations());
      }
      setDashboardError(null);
    } catch (error) {
      const message = errorMessage(error);
      setDashboardError(message);
      if (showSpinner) notify("error", message);
    } finally {
      refreshInFlight.current = false;
      setInitialLoading(false);
      if (showSpinner) setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void refreshData();
    const timer = window.setInterval(() => void refreshData(), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [refreshData]);

  useEffect(() => () => {
    if (toastTimer.current !== null) window.clearTimeout(toastTimer.current);
  }, []);

  function notify(kind: "success" | "error", message: string) {
    setToast({ kind, message });
    if (toastTimer.current !== null) window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(null), 4_200);
  }

  const totals = useMemo(() => instances.reduce((accumulator, instance) => ({
    groups: accumulator.groups + 1,
    bots: accumulator.bots + instance.botSummary.totalBots,
    online: accumulator.online + instance.botSummary.onlineBots,
    starting: accumulator.starting + instance.botSummary.startingBots,
    retrying: accumulator.retrying + instance.botSummary.retryingBots,
    failed: accumulator.failed + instance.botSummary.failedBots,
  }), { groups: 0, bots: 0, online: 0, starting: 0, retrying: 0, failed: 0 }), [instances]);

  const busyInstanceIds = useMemo(() => new Set(
    operations
      .filter((operation) => operation.status === "queued" || operation.status === "running")
      .flatMap((operation) => operation.targets
        .filter((target) => target.status === "queued" || target.status === "running")
        .map((target) => target.instanceId)),
  ), [operations]);

  const filteredInstances = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return instances.filter((instance) => {
      const summary = instance.botSummary;
      const matchesSearch = !normalizedSearch
        || instance.name.toLowerCase().includes(normalizedSearch)
        || instance.serverAddress?.toLowerCase().includes(normalizedSearch);
      const active = summary.desiredBots > 0 || summary.onlineBots > 0
        || summary.startingBots > 0 || summary.retryingBots > 0;
      const matchesFilter = filter === "all" || (filter === "active" ? active : !active);
      return matchesSearch && matchesFilter;
    });
  }, [instances, search, filter]);

  async function connect(baseUrl: string, token: string) {
    const result = await api.connect(baseUrl, token);
    setSession({ connected: true, baseUrl: result.baseUrl });
    setConnectionOpen(false);
    notify("success", `Подключено к SoulFire. Найдено групп: ${result.instanceCount}.`);
    await refreshData();
  }

  async function disconnect() {
    try {
      await api.disconnect();
      setSession({ connected: false });
      setInstances([]);
      setConnectionOpen(false);
      notify("success", "Подключение закрыто. API-токен удалён из памяти панели.");
    } catch (error) {
      notify("error", errorMessage(error));
    }
  }

  async function refreshNow() {
    await refreshData(true);
  }

  async function submitAction(action: FleetAction, targets: InstanceSummary[]) {
    if (targets.length === 0) return;
    try {
      await api.operate(action, targets.map((instance) => ({
        instanceId: instance.id,
        label: instance.name,
      })));
      setConfirmAction(null);
      notify("success", action === "start"
        ? `Команда запуска отправлена для ${targets.length} ${pluralize(targets.length, "группы", "групп", "групп")}.`
        : `Команда остановки отправлена для ${targets.length} ${pluralize(targets.length, "группы", "групп", "групп")} — панель продолжит отвечать.`);
      const nextOperations = await api.operations();
      setOperations(nextOperations);
    } catch (error) {
      notify("error", errorMessage(error));
    }
  }

  async function handleCreateGroup(input: {
    name: string;
    serverAddress: string;
    botNames: string[];
    authorized: boolean;
  }) {
    const result = await api.createGroup(input);
    setNewGroupOpen(false);
    notify("success", `Группа создана. Добавлено offline-ботов: ${result.botCount}.`);
    await refreshData();
  }

  const isConnected = session?.connected === true;
  const currentDetailsGroup = detailsGroup
    ? instances.find((instance) => instance.id === detailsGroup.id) ?? detailsGroup
    : null;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-lockup">
          <div className="brand-mark"><Command size={19} strokeWidth={2.4} /></div>
          <div>
            <div className="brand-name">soulfire<span>fleet</span></div>
            <div className="brand-caption">CONTROL CENTER</div>
          </div>
        </div>

        <div className="sidebar-section-label">РАБОЧЕЕ ПРОСТРАНСТВО</div>
        <nav className="side-nav" aria-label="Основная навигация">
          <button
            className={`nav-item ${activeView === "servers" ? "active" : ""}`}
            onClick={() => setActiveView("servers")}
          >
            <Server size={17} />
            <span>Серверы</span>
            {isConnected && <span className="nav-count">{instances.length}</span>}
          </button>
          <button
            className={`nav-item ${activeView === "activity" ? "active" : ""}`}
            onClick={() => setActiveView("activity")}
          >
            <Activity size={17} />
            <span>Активность</span>
            {operations.some((operation) => operation.status === "running" || operation.status === "queued")
              && <span className="nav-live" />}
          </button>
        </nav>

        <div className="sidebar-section-label sidebar-section-spaced">УПРАВЛЕНИЕ</div>
        <nav className="side-nav" aria-label="Управление">
          <button className="nav-item" onClick={() => setConnectionOpen(true)}>
            <Settings2 size={17} />
            <span>Подключение</span>
          </button>
          <a className="nav-item nav-link" href="https://soulfiremc.com/docs/" target="_blank" rel="noreferrer">
            <CircleHelp size={17} />
            <span>Документация</span>
            <ExternalLink className="nav-external" size={13} />
          </a>
        </nav>

        <div className="sidebar-spacer" />
        <div className={`connection-card ${isConnected ? "is-connected" : ""}`}>
          <div className="connection-card-top">
            <span className="connection-dot" />
            <span>{isConnected ? "SoulFire подключён" : "Нет подключения"}</span>
          </div>
          <div className="connection-address" title={session?.baseUrl || ""}>
            {isConnected ? compactAddress(session?.baseUrl || "") : "Подключите backend для начала"}
          </div>
          <button className="connection-manage" onClick={() => setConnectionOpen(true)}>
            {isConnected ? "Настроить" : "Подключить"}
            <ChevronDown size={14} />
          </button>
        </div>

        <div className="sidebar-footer">
          <span className="footer-led" />
          <span>FLEET CONSOLE</span>
          <span className="footer-version">v0.1</span>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumb"><span>WORKSPACE</span><span className="breadcrumb-separator">/</span><strong>{activeView === "servers" ? "Серверы" : "Активность"}</strong></div>
          <div className="topbar-actions">
            <div className={`backend-pill ${isConnected ? "online" : "offline"}`}>
              {isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
              <span>{isConnected ? "Backend online" : "Backend offline"}</span>
            </div>
            <button className="icon-button" onClick={() => void refreshNow()} disabled={refreshing} aria-label="Обновить данные" title="Обновить">
              <RefreshCw size={16} className={refreshing ? "spin" : ""} />
            </button>
            {isConnected && (
              <button className="topbar-connect" onClick={() => setConnectionOpen(true)}>
                <Settings2 size={15} /> Подключение
              </button>
            )}
          </div>
        </header>

        <main className="page-content">
          {!isConnected ? (
            <ConnectionLanding
              loading={initialLoading}
              onConnect={connect}
              error={dashboardError}
              onOpenConnection={() => setConnectionOpen(true)}
            />
          ) : activeView === "servers" ? (
            <>
              <div className="page-heading-row">
                <div>
                  <div className="eyebrow"><span className="eyebrow-line" /> SOULFIRE FLEET CONTROL</div>
                  <h1>Все серверы,<br className="mobile-break" /> <span>под контролем.</span></h1>
                  <p className="page-description">Управляйте группами ботов на разных Minecraft-серверах независимо друг от друга.</p>
                </div>
                <div className="heading-actions">
                  <button className="button button-secondary" onClick={() => setNewGroupOpen(true)}>
                    <Plus size={16} /> Новая группа
                  </button>
                </div>
              </div>

              {dashboardError && <InlineNotice tone="warning" message={dashboardError} onDismiss={() => setDashboardError(null)} />}

              <section className="metric-grid" aria-label="Общая статистика">
                <MetricCard label="СЕРВЕРНЫЕ ГРУППЫ" value={totals.groups} icon={<Server size={17} />} accent="lime" detail="Отдельные SoulFire-инстансы" />
                <MetricCard label="ВСЕГО БОТОВ" value={totals.bots} icon={<Bot size={17} />} accent="blue" detail="Профили во всех группах" />
                <MetricCard label="СЕЙЧАС В СЕТИ" value={totals.online} icon={<Signal size={17} />} accent="green" detail={totals.bots ? `${Math.round((totals.online / totals.bots) * 100)}% от общего числа` : "Ожидают запуска"} />
                <MetricCard label="ПОДКЛЮЧАЮТСЯ / ОШИБКИ" value={totals.starting + totals.retrying + totals.failed} icon={<Activity size={17} />} accent={totals.failed ? "red" : "amber"} detail={`${totals.starting + totals.retrying} в процессе · ${totals.failed} с ошибкой`} />
              </section>

              <section className="fleet-section">
                <div className="section-heading">
                  <div>
                    <div className="section-title-line">
                      <h2>Серверные группы</h2>
                      <span className="count-badge">{instances.length.toString().padStart(2, "0")}</span>
                    </div>
                    <p>Каждый инстанс SoulFire — отдельная цель и отдельный пул ботов.</p>
                  </div>
                  <div className="global-controls">
                    <button
                      className="button button-quiet stop-all-button"
                      onClick={() => setConfirmAction({ action: "stop", targets: instances })}
                      disabled={!instances.some((instance) => instance.botSummary.desiredBots > 0 || instance.botSummary.onlineBots > 0 || instance.botSummary.startingBots > 0) || instances.length === 0}
                    >
                      <ArrowDownToLine size={15} /> Остановить всё
                    </button>
                    <button
                      className="button button-primary start-all-button"
                      onClick={() => setConfirmAction({ action: "start", targets: instances })}
                      disabled={!instances.some((instance) => instance.botSummary.totalBots > 0) || instances.length === 0}
                    >
                      <ArrowUpFromLine size={15} /> Запустить всё
                    </button>
                  </div>
                </div>

                <div className="fleet-toolbar">
                  <label className="search-field">
                    <Search size={16} />
                    <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Найти сервер или группу…" />
                    {search && <button type="button" aria-label="Очистить поиск" onClick={() => setSearch("")}><X size={14} /></button>}
                  </label>
                  <div className="filter-segment" role="group" aria-label="Фильтр серверов">
                    <button className={filter === "all" ? "selected" : ""} onClick={() => setFilter("all")}>Все</button>
                    <button className={filter === "active" ? "selected" : ""} onClick={() => setFilter("active")}>Активные</button>
                    <button className={filter === "idle" ? "selected" : ""} onClick={() => setFilter("idle")}>Остановлены</button>
                  </div>
                </div>

                {initialLoading ? (
                  <div className="loading-state"><LoaderCircle className="spin" size={24} /><span>Собираем данные SoulFire…</span></div>
                ) : filteredInstances.length > 0 ? (
                  <div className="server-grid">
                    {filteredInstances.map((instance, index) => (
                      <ServerCard
                        key={instance.id}
                        instance={instance}
                        index={index}
                        busy={busyInstanceIds.has(instance.id)}
                        onOpen={() => setDetailsGroup(instance)}
                        onAction={(action) => void submitAction(action, [instance])}
                      />
                    ))}
                  </div>
                ) : instances.length === 0 ? (
                  <EmptyState onCreate={() => setNewGroupOpen(true)} />
                ) : (
                  <div className="empty-filter">
                    <Search size={18} />
                    <strong>Ничего не найдено</strong>
                    <span>Измените запрос или фильтр.</span>
                    <button className="text-button" onClick={() => { setSearch(""); setFilter("all"); }}>Сбросить фильтры</button>
                  </div>
                )}
              </section>

              <section className="lower-grid">
                <div className="panel operation-panel">
                  <div className="panel-heading">
                    <div><span className="panel-icon panel-icon-purple"><Activity size={16} /></span><div><h3>Последние команды</h3><p>Состояние задач управления</p></div></div>
                    <button className="text-button" onClick={() => setActiveView("activity")}>История <ArrowUpFromLine size={13} /></button>
                  </div>
                  <OperationList operations={operations.slice(0, 4)} compact />
                </div>
                <div className="panel architecture-panel">
                  <div className="panel-heading">
                    <div><span className="panel-icon panel-icon-lime"><Zap size={16} /></span><div><h3>Почему панель не зависает</h3><p>Управление вынесено из UI-потока</p></div></div>
                  </div>
                  <div className="architecture-points">
                    <div><span className="architecture-number">01</span><span>Команды отправляются асинхронно, отдельной задачей.</span></div>
                    <div><span className="architecture-number">02</span><span>Остановка имеет приоритет и не ждёт очередь запуска.</span></div>
                    <div><span className="architecture-number">03</span><span>SoulFire сам подключает и отключает ботов в фоне.</span></div>
                  </div>
                </div>
              </section>
            </>
          ) : (
            <ActivityView operations={operations} onBack={() => setActiveView("servers")} />
          )}
        </main>

        <footer className="main-footer">
          <span><span className="footer-led" /> Состояние обновляется автоматически</span>
          <span>Клиентский UI не блокируется сетевыми операциями</span>
        </footer>
      </div>

      {connectionOpen && (
        <ConnectionModal
          currentUrl={session?.baseUrl}
          connected={isConnected}
          onClose={() => setConnectionOpen(false)}
          onConnect={connect}
          onDisconnect={() => void disconnect()}
        />
      )}
      {newGroupOpen && (
        <NewGroupModal
          onClose={() => setNewGroupOpen(false)}
          onCreate={handleCreateGroup}
        />
      )}
      {currentDetailsGroup && (
        <GroupDetailsModal
          instance={currentDetailsGroup}
          busy={busyInstanceIds.has(currentDetailsGroup.id)}
          onClose={() => setDetailsGroup(null)}
          onAction={(action) => void submitAction(action, [currentDetailsGroup])}
          onControlBot={(bot) => setControlBot({ instance: currentDetailsGroup, bot })}
        />
      )}
      {controlBot && (
        <BotControlModal
          instance={controlBot.instance}
          bot={controlBot.bot}
          onClose={() => setControlBot(null)}
        />
      )}
      {confirmAction && (
        <ConfirmModal
          action={confirmAction.action}
          targets={confirmAction.targets}
          busy={confirmAction.targets.some((target) => busyInstanceIds.has(target.id))}
          onCancel={() => setConfirmAction(null)}
          onConfirm={() => void submitAction(confirmAction.action, confirmAction.targets)}
        />
      )}
      {toast && <div className={`toast toast-${toast.kind}`} role="status"><span className="toast-icon">{toast.kind === "success" ? <CheckCircle2 size={17} /> : <AlertTriangle size={17} />}</span><span>{toast.message}</span><button onClick={() => setToast(null)} aria-label="Закрыть уведомление"><X size={15} /></button></div>}
    </div>
  );
}

function ConnectionLanding({
  loading,
  onConnect,
  error,
  onOpenConnection,
}: {
  loading: boolean;
  onConnect: (baseUrl: string, token: string) => Promise<void>;
  error: string | null;
  onOpenConnection: () => void;
}) {
  return (
    <div className="landing-wrap">
      <div className="landing-glow landing-glow-one" />
      <div className="landing-glow landing-glow-two" />
      <div className="landing-copy">
        <div className="eyebrow"><span className="eyebrow-line" /> MULTI-SERVER MANAGEMENT</div>
        <h1>Боты под<br /><span>контролем.</span></h1>
        <p>Одна панель для нескольких серверов. Запуск и остановка не блокируют интерфейс — даже если ботов много.</p>
        <div className="landing-points">
          <div><Check size={15} /><span>Отдельная группа на каждый SoulFire-инстанс</span></div>
          <div><Check size={15} /><span>Приоритетная остановка и очередь запуска</span></div>
          <div><Check size={15} /><span>POV и ручное управление выбранным ботом</span></div>
        </div>
      </div>
      <div className="landing-connect-card">
        <div className="landing-card-top"><span className="landing-lock"><LockKeyhole size={16} /></span><span className="overline">ПЕРВЫЙ ШАГ</span></div>
        <h2>Подключитесь к SoulFire</h2>
        <p>Нужен API-токен с доступом к нужным инстансам.</p>
        {error && <InlineNotice tone="warning" message={error} />}
        <ConnectionForm onConnect={onConnect} connected={false} compact />
        <button className="landing-secondary-link" onClick={onOpenConnection}>Открыть настройки подключения <ExternalLink size={13} /></button>
        <div className="landing-security"><ShieldCheck size={15} /><span>Токен хранится только в памяти backend-процесса и не записывается в localStorage.</span></div>
        {loading && <div className="landing-checking"><LoaderCircle size={13} className="spin" /> Проверяем состояние панели…</div>}
      </div>
      <div className="landing-grid-mark" aria-hidden="true" />
    </div>
  );
}

function MetricCard({ label, value, icon, accent, detail }: {
  label: string;
  value: number;
  icon: React.ReactNode;
  accent: "lime" | "blue" | "green" | "amber" | "red";
  detail: string;
}) {
  return (
    <div className={`metric-card metric-${accent}`}>
      <div className="metric-top"><span>{label}</span><span className="metric-icon">{icon}</span></div>
      <div className="metric-value">{value.toLocaleString("ru-RU")}</div>
      <div className="metric-detail">{detail}</div>
      <div className="metric-decor" />
    </div>
  );
}

function ServerCard({
  instance,
  index,
  busy,
  onOpen,
  onAction,
}: {
  instance: InstanceSummary;
  index: number;
  busy: boolean;
  onOpen: () => void;
  onAction: (action: FleetAction) => void;
}) {
  const { botSummary } = instance;
  const isActive = botSummary.desiredBots > 0 || botSummary.onlineBots > 0
    || botSummary.startingBots > 0 || botSummary.retryingBots > 0;
  const completion = botSummary.totalBots > 0 ? botSummary.onlineBots / botSummary.totalBots * 100 : 0;
  const statusLabel = botSummary.totalBots === 0
    ? "Нет ботов"
    : botSummary.failedBots > 0 && botSummary.onlineBots === 0
      ? "Есть ошибки"
      : isActive
        ? botSummary.onlineBots === botSummary.totalBots ? "Все в сети" : "Работает"
        : "Остановлен";
  const statusTone = botSummary.failedBots > 0 && botSummary.onlineBots === 0 ? "danger" : isActive ? "online" : "idle";

  return (
    <article className={`server-card ${isActive ? "server-card-active" : ""}`} style={{ "--card-index": index } as React.CSSProperties}>
      <div className="server-card-top">
        <button className="server-avatar" onClick={onOpen} aria-label={`Открыть ${instance.name}`}>
          <span>{instance.name.trim().charAt(0).toUpperCase() || "S"}</span>
          <i className={statusTone} />
        </button>
        <div className="server-title-block">
          <button className="server-name" onClick={onOpen}>{instance.name}</button>
          <div className="server-address"><span className="address-dot" />{instance.serverAddress || "Адрес задан в конфигурации SoulFire"}</div>
        </div>
        <button className={`status-chip status-${statusTone}`} onClick={onOpen} title="Посмотреть ботов">
          <span />{statusLabel}
        </button>
      </div>

      <div className="server-card-stats">
        <div className="online-stat"><span className="online-big">{botSummary.onlineBots.toString().padStart(2, "0")}</span><span className="online-divider">/</span><span className="online-total">{botSummary.totalBots.toString().padStart(2, "0")}</span><span className="online-label">В СЕТИ</span></div>
        <div className="server-extra-stats">
          <div><span className="extra-dot starting-dot" />Подключаются <strong>{botSummary.startingBots}</strong></div>
          <div><span className="extra-dot retrying-dot" />Повторяют вход <strong>{botSummary.retryingBots}</strong></div>
          {botSummary.failedBots > 0 && <div><span className="extra-dot failed-dot" />Ошибки <strong>{botSummary.failedBots}</strong></div>}
        </div>
      </div>

      <div className="progress-row"><div className="progress-track"><div className={`progress-fill ${botSummary.failedBots > 0 ? "has-failed" : ""}`} style={{ width: `${Math.min(100, completion)}%` }} /></div><span>{Math.round(completion)}%</span></div>

      <div className="server-card-bottom">
        <span className="server-id">INSTANCE <strong>{instance.id.slice(0, 8).toUpperCase()}</strong></span>
        <div className="server-card-actions">
          <button className="details-button" onClick={onOpen}>Боты <span>{botSummary.totalBots}</span></button>
          <button
            className={`button ${isActive ? "button-stop" : "button-primary card-start-button"}`}
            onClick={() => onAction(isActive ? "stop" : "start")}
            disabled={busy || botSummary.totalBots === 0}
            title={busy ? "Команда уже в очереди" : undefined}
          >
            {busy ? <LoaderCircle className="spin" size={15} /> : isActive ? <ArrowDownToLine size={15} /> : <ArrowUpFromLine size={15} />}
            {busy ? "В очереди" : isActive ? "Остановить" : "Запустить"}
          </button>
        </div>
      </div>
    </article>
  );
}

function EmptyState({ onCreate }: { onCreate: () => void }) {
  return (
    <div className="empty-state">
      <div className="empty-orbit"><Server size={22} /><span /><i /></div>
      <div className="eyebrow"><span className="eyebrow-line" /> ГОТОВЫ К ЗАПУСКУ</div>
      <h3>Пока нет серверных групп</h3>
      <p>Создайте отдельный SoulFire-инстанс для каждого сервера. После этого группы можно будет запускать и останавливать независимо.</p>
      <button className="button button-primary" onClick={onCreate}><Plus size={16} /> Добавить первый сервер</button>
    </div>
  );
}

function ConnectionModal({
  currentUrl,
  connected,
  onClose,
  onConnect,
  onDisconnect,
}: {
  currentUrl?: string;
  connected: boolean;
  onClose: () => void;
  onConnect: (baseUrl: string, token: string) => Promise<void>;
  onDisconnect: () => void;
}) {
  return (
    <Modal onClose={onClose} wide={false}>
      <div className="modal-heading-icon"><Settings2 size={18} /></div>
      <div className="modal-eyebrow">SOULFIRE API</div>
      <h2>{connected ? "Настройки подключения" : "Подключение к SoulFire"}</h2>
      <p className="modal-description">Панель соединяется с backend SoulFire от имени сервера, на котором запущен этот веб-интерфейс.</p>
      <ConnectionForm onConnect={onConnect} initialUrl={currentUrl} connected={connected} />
      {connected && (
        <div className="connection-modal-bottom">
          <span><span className="connection-dot" /> Активно: {compactAddress(currentUrl || "")}</span>
          <button className="text-button danger-text" onClick={onDisconnect}><LogOut size={14} /> Отключить</button>
        </div>
      )}
      <div className="modal-note"><LockKeyhole size={14} /><span>API-токен остаётся в памяти backend-процесса. Для удалённого SoulFire используйте HTTPS.</span></div>
    </Modal>
  );
}

function ConnectionForm({
  onConnect,
  initialUrl,
  connected,
  compact = false,
}: {
  onConnect: (baseUrl: string, token: string) => Promise<void>;
  initialUrl?: string;
  connected: boolean;
  compact?: boolean;
}) {
  const [baseUrl, setBaseUrl] = useState(initialUrl || "http://127.0.0.1:38765");
  const [token, setToken] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onConnect(baseUrl, token);
      setToken("");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className={`connection-form ${compact ? "connection-form-compact" : ""}`} onSubmit={handleSubmit}>
      <label>
        <span>Адрес SoulFire</span>
        <div className="input-with-icon"><Server size={15} /><input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} placeholder="http://127.0.0.1:38765" autoComplete="url" required /></div>
      </label>
      <label>
        <span>API-токен</span>
        <input className="token-input" type="password" value={token} onChange={(event) => setToken(event.target.value)} placeholder={connected ? "Введите токен повторно для смены подключения" : "Вставьте токен из SoulFire console"} autoComplete="new-password" required />
      </label>
      {error && <div className="form-error"><AlertTriangle size={14} />{error}</div>}
      <button className="button button-primary connect-submit" type="submit" disabled={busy || !token.trim()}>
        {busy ? <><LoaderCircle className="spin" size={15} /> Проверяем доступ…</> : <><Wifi size={15} /> Подключить backend</>}
      </button>
      {!compact && <p className="form-footnote">Токен создаётся в консоли SoulFire командой <code>generate-token api</code>.</p>}
    </form>
  );
}

function NewGroupModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (input: { name: string; serverAddress: string; botNames: string[]; authorized: boolean }) => Promise<void>;
}) {
  const [name, setName] = useState("");
  const [serverAddress, setServerAddress] = useState("");
  const [botNamesText, setBotNamesText] = useState("");
  const [authorized, setAuthorized] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const botNames = botNamesText.split(/[\n,;]+/).map((part) => part.trim()).filter(Boolean);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onCreate({ name, serverAddress, botNames, authorized });
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose} wide>
      <div className="modal-heading-icon modal-heading-lime"><Plus size={19} /></div>
      <div className="modal-eyebrow">НОВАЯ ЦЕЛЬ</div>
      <h2>Добавить серверную группу</h2>
      <p className="modal-description">Создадим отдельный SoulFire-инстанс для сервера и добавим offline-профили. Уже импортированные аккаунты можно управлять из панели после подключения.</p>
      <form className="group-form" onSubmit={handleSubmit}>
        <div className="form-row">
          <label><span>Название группы</span><input value={name} onChange={(event) => setName(event.target.value)} placeholder="Например, Paper staging" maxLength={64} required /></label>
          <label><span>Адрес Minecraft-сервера</span><input value={serverAddress} onChange={(event) => setServerAddress(event.target.value)} placeholder="test.example.net:25565" maxLength={255} required /></label>
        </div>
        <label className="full-label"><span>Имена offline-ботов <em>необязательно</em></span><textarea value={botNamesText} onChange={(event) => setBotNamesText(event.target.value)} placeholder={"TestBot_01\nTestBot_02\nTestBot_03"} rows={4} /></label>
        <div className="form-help"><Bot size={14} /><span>Имена разделяйте переносом строки или запятой. До 100 профилей за раз. Microsoft-аккаунты импортируйте в SoulFire — они появятся здесь автоматически.</span></div>
        <label className="permission-check"><input type="checkbox" checked={authorized} onChange={(event) => setAuthorized(event.target.checked)} /><span>Я владелец этого сервера или имею разрешение на тестирование.</span></label>
        {error && <div className="form-error"><AlertTriangle size={14} />{error}</div>}
        <div className="modal-actions">
          <button className="button button-quiet" type="button" onClick={onClose}>Отмена</button>
          <button className="button button-primary" type="submit" disabled={busy || !authorized}>
            {busy ? <><LoaderCircle className="spin" size={15} /> Создаём…</> : <><Plus size={15} /> Создать группу{botNames.length ? ` · ${botNames.length} ${pluralize(botNames.length, "бот", "бота", "ботов")}` : ""}</>}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function GroupDetailsModal({
  instance,
  busy,
  onClose,
  onAction,
  onControlBot,
}: {
  instance: InstanceSummary;
  busy: boolean;
  onClose: () => void;
  onAction: (action: FleetAction) => void;
  onControlBot: (bot: BotRow) => void;
}) {
  const [bots, setBots] = useState<BotRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [botSearch, setBotSearch] = useState("");
  const botsRequestInFlight = useRef(false);
  const isActive = instance.botSummary.desiredBots > 0 || instance.botSummary.onlineBots > 0
    || instance.botSummary.startingBots > 0 || instance.botSummary.retryingBots > 0;

  const loadBots = useCallback(async (showLoading = false) => {
    if (botsRequestInFlight.current) return;
    botsRequestInFlight.current = true;
    if (showLoading) setLoading(true);
    try {
      setBots(await api.bots(instance.id));
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      botsRequestInFlight.current = false;
      setLoading(false);
    }
  }, [instance.id]);

  useEffect(() => {
    void loadBots(true);
    const timer = window.setInterval(() => void loadBots(false), REFRESH_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [loadBots]);

  const visibleBots = bots.filter((bot) => bot.name.toLowerCase().includes(botSearch.trim().toLowerCase()));

  return (
    <Modal onClose={onClose} wide>
      <div className="details-modal-top">
        <div className="details-avatar"><span>{instance.name.charAt(0).toUpperCase() || "S"}</span><i className={isActive ? "online" : "idle"} /></div>
        <div className="details-heading">
          <div className="modal-eyebrow">INSTANCE {instance.id.slice(0, 8).toUpperCase()}</div>
          <h2>{instance.name}</h2>
          <p><Server size={14} /> {instance.serverAddress || "Адрес сервера хранится в настройках SoulFire"}</p>
        </div>
        <button className="icon-button modal-close" onClick={onClose} aria-label="Закрыть"><X size={17} /></button>
      </div>

      <div className="detail-metrics">
        <div><span>БОТЫ</span><strong>{instance.botSummary.totalBots}</strong></div>
        <div className="detail-metric-online"><span>В СЕТИ</span><strong>{instance.botSummary.onlineBots}</strong></div>
        <div><span>ПОДКЛЮЧАЮТСЯ</span><strong>{instance.botSummary.startingBots}</strong></div>
        <div className={instance.botSummary.failedBots ? "detail-metric-error" : ""}><span>ОШИБКИ</span><strong>{instance.botSummary.failedBots}</strong></div>
      </div>

      <div className="details-list-heading">
        <div><h3>Профили ботов</h3><span>{bots.length} в этом инстансе</span></div>
        <label className="search-field bot-search"><Search size={15} /><input value={botSearch} onChange={(event) => setBotSearch(event.target.value)} placeholder="Найти бота…" /></label>
      </div>

      <div className="bot-list-wrap">
        {loading ? <div className="bot-list-loading"><LoaderCircle className="spin" size={18} /> Загружаем состояние…</div>
          : error ? <InlineNotice tone="warning" message={error} onDismiss={() => setError(null)} />
            : visibleBots.length ? <div className="bot-list">
              {visibleBots.map((bot) => <BotListRow key={bot.id} bot={bot} onControl={() => onControlBot(bot)} />)}
            </div>
              : <div className="bot-empty"><Bot size={20} /><span>{bots.length ? "Нет совпадений по запросу." : "В этой группе пока нет профилей."}</span></div>}
      </div>

      <div className="details-footer">
        <span><Radio size={14} /> Статус от SoulFire · обновите панель для синхронизации</span>
        <button className={`button ${isActive ? "button-stop" : "button-primary"}`} onClick={() => onAction(isActive ? "stop" : "start")} disabled={busy || instance.botSummary.totalBots === 0}>
          {busy ? <LoaderCircle className="spin" size={15} /> : isActive ? <ArrowDownToLine size={15} /> : <ArrowUpFromLine size={15} />}
          {busy ? "Команда в очереди" : isActive ? "Остановить группу" : "Запустить группу"}
        </button>
      </div>
    </Modal>
  );
}

function BotListRow({ bot, onControl }: { bot: BotRow; onControl: () => void }) {
  const status = bot.runtimeState === "FAILED" ? "error"
    : bot.online || bot.runtimeState === "RUNNING" ? "online"
      : bot.runtimeState === "STARTING" || bot.runtimeState === "QUEUED" || bot.runtimeState === "RETRYING" || bot.runtimeState === "STOPPING" ? "pending"
        : "idle";
  const statusLabel = status === "error" ? "Ошибка"
    : status === "online" ? "В сети"
      : status === "pending" ? bot.runtimeState === "RETRYING" ? "Переподключение" : "Подключается"
        : "Остановлен";
  return (
    <div className="bot-row">
      <span className={`bot-status-dot bot-${status}`} />
      <div className="bot-row-name"><strong>{bot.name}</strong><span title={bot.id}>{bot.id.slice(0, 8).toUpperCase()}</span></div>
      <div className={`bot-state-label bot-state-${status}`}>{statusLabel}</div>
      <div className="bot-row-meta">{bot.online && bot.pingMs !== undefined ? <><Signal size={13} /> {bot.pingMs} ms</> : bot.dimension ? bot.dimension.split(":").at(-1) : "—"}</div>
      <button className="bot-control-button" onClick={onControl} disabled={!bot.online} title={bot.online ? "Открыть POV и ручное управление" : "Бот должен быть в сети"}><Camera size={13} /> Экран</button>
      {bot.lastError && <div className="bot-row-error" title={bot.lastError}><AlertTriangle size={13} /> <span>{bot.lastError}</span></div>}
    </div>
  );
}

function BotControlModal({
  instance,
  bot,
  onClose,
}: {
  instance: InstanceSummary;
  bot: BotRow;
  onClose: () => void;
}) {
  const [frameUrl, setFrameUrl] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [streamLoaded, setStreamLoaded] = useState(false);
  const [streamVersion, setStreamVersion] = useState(0);
  const [capturing, setCapturing] = useState(false);
  const [captureError, setCaptureError] = useState<string | null>(null);
  const [moveBusy, setMoveBusy] = useState(false);
  const [lookBusy, setLookBusy] = useState(false);
  const [durationMs, setDurationMs] = useState(350);
  const [yaw, setYaw] = useState(bot.yaw ?? 0);
  const [pitch, setPitch] = useState(bot.pitch ?? 0);
  const [lastFrameAt, setLastFrameAt] = useState<Date | null>(null);
  const frameInFlight = useRef(false);
  const objectUrl = useRef<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadFrame = useCallback(async (showSpinner = false) => {
    if (frameInFlight.current || !bot.online) return;
    frameInFlight.current = true;
    if (showSpinner) setCapturing(true);
    try {
      const image = await api.pov(instance.id, bot.id);
      const nextUrl = URL.createObjectURL(image);
      const previousUrl = objectUrl.current;
      objectUrl.current = nextUrl;
      setFrameUrl(nextUrl);
      setLastFrameAt(new Date());
      setCaptureError(null);
      if (previousUrl) URL.revokeObjectURL(previousUrl);
    } catch (error) {
      setCaptureError(errorMessage(error));
    } finally {
      frameInFlight.current = false;
      setCapturing(false);
    }
  }, [bot.id, bot.online, instance.id]);

  useEffect(() => {
    if (!bot.online) setCaptureError("Бот не в сети. Запустите его, чтобы открыть POV.");
    else setCaptureError(null);
  }, [bot.online]);

  useEffect(() => () => {
    if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
  }, []);

  function refreshPov() {
    setCaptureError(null);
    if (autoRefresh) {
      setStreamLoaded(false);
      setStreamVersion((version) => version + 1);
    } else {
      void loadFrame(true);
    }
  }

  function toggleAutoRefresh(enabled: boolean) {
    setAutoRefresh(enabled);
    setCaptureError(null);
    setStreamLoaded(false);
    if (enabled) {
      setStreamVersion((version) => version + 1);
    } else {
      setFrameUrl(null);
      void loadFrame(true);
    }
  }

  async function move(key: "forward" | "backward" | "left" | "right" | "jump" | "sneak" | "sprint") {
    setMoveBusy(true);
    setActionError(null);
    try {
      await api.moveBot(instance.id, bot.id, key, durationMs);
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setMoveBusy(false);
    }
  }

  async function turn(deltaYaw: number, deltaPitch: number) {
    const nextYaw = ((yaw + deltaYaw + 540) % 360) - 180;
    const nextPitch = Math.max(-90, Math.min(90, pitch + deltaPitch));
    setLookBusy(true);
    setActionError(null);
    try {
      await api.lookBot(instance.id, bot.id, nextYaw, nextPitch);
      setYaw(nextYaw);
      setPitch(nextPitch);
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setLookBusy(false);
    }
  }

  async function resetMovement() {
    setMoveBusy(true);
    setActionError(null);
    try {
      await api.resetBot(instance.id, bot.id);
    } catch (error) {
      setActionError(errorMessage(error));
    } finally {
      setMoveBusy(false);
    }
  }

  function handleClose() {
    void api.resetBot(instance.id, bot.id).catch(() => undefined);
    onClose();
  }

  return (
    <Modal onClose={handleClose} wide className="control-modal-card">
      <div className="control-modal-heading">
        <div className="details-avatar"><span>{bot.name.charAt(0).toUpperCase() || "B"}</span><i className="online" /></div>
        <div className="details-heading">
          <div className="modal-eyebrow">POV · {instance.name}</div>
          <h2>{bot.name}</h2>
          <p><Signal size={13} /> {bot.pingMs !== undefined ? `${bot.pingMs} ms` : "бот в сети"} · ручное управление</p>
        </div>
        <button className="button button-quiet control-refresh" onClick={refreshPov} disabled={capturing}>
          <RefreshCw size={14} className={capturing ? "spin" : ""} /> {autoRefresh ? "Переподключить" : "Кадр"}
        </button>
      </div>

      <div className="control-layout">
        <div className="pov-panel">
          <div className="pov-screen">
            {autoRefresh ? (
              <>
                <img
                  src={`${api.povStream(instance.id, bot.id)}?stream=${streamVersion}`}
                  alt={`POV бота ${bot.name}`}
                  draggable={false}
                  onLoad={() => { setStreamLoaded(true); setLastFrameAt(new Date()); setCaptureError(null); }}
                  onError={() => { setStreamLoaded(false); setCaptureError("Поток POV недоступен. Проверьте соединение SoulFire и перезапустите поток."); }}
                />
                {!streamLoaded && <div className="pov-placeholder"><Camera size={25} /><span>Подключаем поток SoulFire…</span></div>}
              </>
            ) : frameUrl ? (
              <img src={frameUrl} alt={`POV бота ${bot.name}`} draggable={false} />
            ) : (
              <div className="pov-placeholder"><Camera size={25} /><span>{capturing ? "Рендерим кадр…" : "Ожидаем кадр SoulFire"}</span></div>
            )}
            <div className="pov-overlay"><span><i /> {autoRefresh ? "LIVE PREVIEW" : "SNAPSHOT"}</span><span>{lastFrameAt ? lastFrameAt.toLocaleTimeString("ru-RU", { minute: "2-digit", second: "2-digit" }) : "—"}</span></div>
          </div>
          {captureError && <InlineNotice tone="warning" message={captureError} />}
          <div className="pov-footer"><span>{autoRefresh ? "Поток SoulFire · около 1 кадра/с" : "PNG-снимок · 960 × 540"}</span><label><input type="checkbox" checked={autoRefresh} onChange={(event) => toggleAutoRefresh(event.target.checked)} /> поток POV</label></div>
        </div>

        <div className="manual-controls">
          <section className="control-block">
            <div className="control-block-heading"><div><span className="control-block-icon"><ArrowUp size={14} /></span><div><strong>Передвижение</strong><span>Импульс движения</span></div></div>
              <select value={durationMs} onChange={(event) => setDurationMs(Number(event.target.value))} aria-label="Длительность нажатия">
                <option value={200}>200 мс</option><option value={350}>350 мс</option><option value={600}>600 мс</option>
              </select>
            </div>
            <div className="movement-pad" aria-label="Управление движением">
              <span />
              <button onClick={() => void move("forward")} disabled={moveBusy} aria-label="Вперёд"><ArrowUp size={17} /></button>
              <span />
              <button onClick={() => void move("left")} disabled={moveBusy} aria-label="Влево"><ArrowLeft size={17} /></button>
              <button className="movement-stop" onClick={() => void resetMovement()} disabled={moveBusy} aria-label="Стоп"><X size={15} /></button>
              <button onClick={() => void move("right")} disabled={moveBusy} aria-label="Вправо"><ArrowRight size={17} /></button>
              <span />
              <button onClick={() => void move("backward")} disabled={moveBusy} aria-label="Назад"><ArrowDown size={17} /></button>
              <span />
            </div>
            <div className="movement-modifiers">
              <button onClick={() => void move("jump")} disabled={moveBusy}>Прыжок</button>
              <button onClick={() => void move("sneak")} disabled={moveBusy}>Присесть</button>
              <button onClick={() => void move("sprint")} disabled={moveBusy}>Спринт</button>
            </div>
          </section>

          <section className="control-block look-block">
            <div className="control-block-heading"><div><span className="control-block-icon control-look-icon"><RotateCcw size={14} /></span><div><strong>Поворот взгляда</strong><span>Шаг 12° · yaw {Math.round(yaw)}° / pitch {Math.round(pitch)}°</span></div></div></div>
            <div className="look-controls">
              <button onClick={() => void turn(0, -12)} disabled={lookBusy} aria-label="Взгляд вверх"><ArrowUp size={15} /></button>
              <button onClick={() => void turn(-12, 0)} disabled={lookBusy} aria-label="Повернуть влево"><ArrowLeft size={15} /></button>
              <button onClick={() => void turn(12, 0)} disabled={lookBusy} aria-label="Повернуть вправо"><ArrowRight size={15} /></button>
              <button onClick={() => void turn(0, 12)} disabled={lookBusy} aria-label="Взгляд вниз"><ArrowDown size={15} /></button>
            </div>
          </section>
          {actionError && <InlineNotice tone="warning" message={actionError} />}
          <div className="control-safety-note"><ShieldCheck size={14} /><span>Нажатия короткие и автоматически сбрасываются; закрытие окна также отправляет Stop.</span></div>
        </div>
      </div>
    </Modal>
  );
}

function ConfirmModal({
  action,
  targets,
  busy,
  onCancel,
  onConfirm,
}: {
  action: FleetAction;
  targets: InstanceSummary[];
  busy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const totalBots = targets.reduce((sum, target) => sum + target.botSummary.totalBots, 0);
  const stopping = action === "stop";
  return (
    <Modal onClose={onCancel} wide={false}>
      <div className={`modal-heading-icon ${stopping ? "modal-heading-stop" : "modal-heading-lime"}`}>{stopping ? <ArrowDownToLine size={18} /> : <ArrowUpFromLine size={18} />}</div>
      <div className="modal-eyebrow">ПОДТВЕРЖДЕНИЕ</div>
      <h2>{stopping ? "Остановить выбранные группы?" : "Запустить выбранные группы?"}</h2>
      <p className="modal-description">Будет отправлена команда для <strong>{targets.length} {pluralize(targets.length, "группы", "групп", "групп")}</strong> и до <strong>{totalBots} профилей</strong>. Запуск или отключение продолжится на стороне SoulFire в фоне.</p>
      {busy && <InlineNotice tone="warning" message="По некоторым группам уже выполняется команда. Новое действие заменит ожидающие команды." />}
      <div className="confirm-targets">{targets.slice(0, 5).map((target) => <span key={target.id}><span className="connection-dot" />{target.name}</span>)}{targets.length > 5 && <span>+ ещё {targets.length - 5}</span>}</div>
      <div className="modal-actions">
        <button className="button button-quiet" onClick={onCancel}>Отмена</button>
        <button className={`button ${stopping ? "button-stop" : "button-primary"}`} onClick={onConfirm}>{stopping ? <ArrowDownToLine size={15} /> : <ArrowUpFromLine size={15} />}{stopping ? "Остановить" : "Запустить"}</button>
      </div>
    </Modal>
  );
}

function ActivityView({ operations, onBack }: { operations: FleetOperation[]; onBack: () => void }) {
  const activeCount = operations.filter((operation) => operation.status === "queued" || operation.status === "running").length;
  return (
    <div className="activity-page">
      <div className="eyebrow"><span className="eyebrow-line" /> ОЧЕРЕДЬ ЗАДАЧ</div>
      <div className="activity-heading-row"><div><h1>История <span>команд.</span></h1><p className="page-description">Команды не блокируют интерфейс. Остановка запускается отдельной очередью и имеет приоритет над ожидающим стартом.</p></div><button className="button button-secondary" onClick={onBack}><Server size={15} /> К серверам</button></div>
      <div className="activity-summary-row"><div className="activity-summary"><span className="activity-summary-icon"><Activity size={17} /></span><div><span>АКТИВНЫЕ ОПЕРАЦИИ</span><strong>{activeCount}</strong></div></div><div className="activity-summary"><span className="activity-summary-icon activity-summary-green"><CheckCircle2 size={17} /></span><div><span>ЗАПИСЕЙ В ИСТОРИИ</span><strong>{operations.length}</strong></div></div><div className="activity-summary-note"><ShieldCheck size={16} /><span>Операция означает, что SoulFire принял желаемое состояние. Реальное подключение/отключение видно в статусе группы.</span></div></div>
      <div className="panel activity-history-panel"><div className="history-panel-heading"><div><h2>Все операции</h2><p>Последние команды панели</p></div><span className="count-badge">{operations.length.toString().padStart(2, "0")}</span></div><OperationList operations={operations} /></div>
    </div>
  );
}

function OperationList({ operations, compact = false }: { operations: FleetOperation[]; compact?: boolean }) {
  if (operations.length === 0) {
    return <div className={`operation-empty ${compact ? "compact" : ""}`}><Clock3 size={17} /><span>Команд пока нет</span></div>;
  }
  return (
    <div className={`operation-list ${compact ? "operation-list-compact" : ""}`}>
      {operations.map((operation) => {
        const isStart = operation.action === "start";
        const active = operation.status === "queued" || operation.status === "running";
        const failed = operation.status === "partial" || operation.targets.some((target) => target.status === "failed");
        const complete = operation.status === "completed";
        return (
          <div className="operation-item" key={operation.id}>
            <span className={`operation-icon ${isStart ? "operation-start" : "operation-stop"}`}>{isStart ? <ArrowUpFromLine size={14} /> : <ArrowDownToLine size={14} />}</span>
            <div className="operation-description"><strong>{isStart ? "Запуск" : "Остановка"} · {operation.targets.length} {pluralize(operation.targets.length, "группа", "группы", "групп")}</strong><span title={operation.targets.map((target) => target.error ? `${target.label}: ${target.error}` : `${target.label}: ${target.message || target.status}`).join("\n")}>{operation.targets.map((target) => target.error ? `${target.label}: ${target.error}` : target.label).join(", ")}</span></div>
            <div className="operation-meta"><span className={`operation-status ${active ? "pending" : failed ? "failed" : complete ? "success" : "muted"}`}>{active ? "Выполняется" : failed ? "Есть ошибки" : complete ? "Команда принята" : "Отменена"}</span><time>{formatRelative(operation.createdAt)}</time></div>
          </div>
        );
      })}
    </div>
  );
}

function InlineNotice({ tone, message, onDismiss }: { tone: "warning" | "error"; message: string; onDismiss?: () => void }) {
  return <div className={`inline-notice notice-${tone}`}><AlertTriangle size={15} /><span>{message}</span>{onDismiss && <button onClick={onDismiss} aria-label="Закрыть"><X size={14} /></button>}</div>;
}

function Modal({ children, onClose, wide, className = "" }: { children: React.ReactNode; onClose: () => void; wide: boolean; className?: string }) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <div className={`modal-card ${wide ? "modal-wide" : ""} ${className}`} role="dialog" aria-modal="true">
        {children}
        <button className="modal-x" onClick={onClose} aria-label="Закрыть"><X size={16} /></button>
      </div>
    </div>
  );
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Произошла неизвестная ошибка.";
}

function compactAddress(value: string): string {
  try {
    const url = new URL(value);
    return `${url.host}${url.pathname === "/" ? "" : url.pathname}`;
  } catch {
    return value;
  }
}

function pluralize(value: number, one: string, few: string, many: string): string {
  const mod10 = value % 10;
  const mod100 = value % 100;
  if (mod10 === 1 && mod100 !== 11) return one;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
  return many;
}

function formatRelative(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const delta = Math.max(0, Date.now() - date.getTime());
  if (delta < 60_000) return "только что";
  if (delta < 3_600_000) return `${Math.floor(delta / 60_000)} мин. назад`;
  if (delta < 86_400_000) return `${Math.floor(delta / 3_600_000)} ч. назад`;
  return date.toLocaleString("ru-RU", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
}
