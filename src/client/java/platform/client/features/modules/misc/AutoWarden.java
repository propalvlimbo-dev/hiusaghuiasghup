package platform.client.features.modules.misc;

import platform.client.utils.text.StringUtils;
import platform.api.module.Interface;

import static platform.api.module.Interface.aM_;
import platform.client.Delta;
import platform.api.module.InterfaceC0020Opcode;
import platform.api.module.Module;
import platform.client.utils.text.ChatUtil;
import platform.client.utils.player.InventoryUtil;
import platform.client.utils.player.UseItemUtil;
import platform.client.utils.math.MathUtil;
import platform.client.utils.player.ServerUtil;

import platform.api.module.Category;
import platform.api.event.interfaces.EventTarget;
import platform.api.module.ModuleRegister;
import platform.api.event.events.player.InputEvent;
import platform.api.event.events.client.PacketEvent;
import platform.api.event.events.client.SoundEvent;
import platform.api.event.events.client.TickEvent;
import platform.client.features.modules.render.WardenESP;
import platform.client.utils.rotation.Rotation;

import platform.api.module.setting.BooleanSetting;
import platform.client.utils.timer.CounterUtil;
import platform.api.module.setting.ModeSetting;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Generated;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import platform.api.module.Interface;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;

@ModuleRegister(a = "Auto Warden", b = "Автоматизирует фарм варденов на анархии", c = Category.Misc)
public class AutoWarden extends Module {
    private int i;
    private boolean j;
    private AABB k;
    private BlockPos l;
    private String n;
    private final List<Integer> b = new ArrayList();
    private final Map<BlockPos, Integer> c = new HashMap();
    private final Map<BlockPos, Integer> d = new HashMap();
    private final BooleanSetting e = new BooleanSetting("Использовать скорость", false);
    private final BooleanSetting f = new BooleanSetting("Репортить обидчиков", false);
    private final ModeSetting g = new ModeSetting("Приоритеты лута", "Средний", "Низкий", "Средний", "Высокий");
    private int h = 1;
    private final CounterUtil m = new CounterUtil();
    private a o = a.SAVE;

    enum a {
        SAVE,
        TAKE,
        COLLECTING,
        ESCAPE
    }

    @Generated
    public List<Integer> q() {
        return this.b;
    }

    public AutoWarden() {
        a(this.e, this.f, this.g);
    }

    @Override
    public void b() {
        super.b();
        int current = ServerUtil.a.d();
        if (current >= 0) {
            this.b.remove(Integer.valueOf(current));
            this.b.add(0, Integer.valueOf(current));
        }
        this.h = 1;
        this.o = a.COLLECTING;
        this.d.clear();
        if (!Delta.h().d().t().i().m()) {
            Delta.h().d().t().i().a();
        }
        ChatUtil.a((Object) "Shift + Пробел — быстрое выключение функции");
        BaritoneAPI.getSettings().avoidance.value = true;
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = 256;
        BaritoneAPI.getSettings().freeLook.value = true;
        BaritoneAPI.getSettings().blockFreeLook.value = true;
        BaritoneAPI.getSettings().antiCheatCompatibility.value = true;
        BaritoneAPI.getSettings().randomLooking.value = Double.valueOf(1.0d);
        BaritoneAPI.getSettings().randomLooking113.value = Double.valueOf(1.0d);
        d(true);
    }

    @Override
    public void c() {
        super.c();
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        BaritoneAPI.getSettings().allowBreak.value = true;
        BaritoneAPI.getSettings().allowPlace.value = true;
        BaritoneAPI.getSettings().avoidance.value = false;
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = 3;
        UseItemUtil.c();
        this.spotCache.clear();
        this.c.clear();
        d(false);
    }

    private void d(boolean add) {
        List<Block> list = (List) BaritoneAPI.getSettings().blocksToAvoid.value;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().is(BlockTags.CANDLES)) {
                if (!add) {
                    list.remove(block);
                } else if (!list.contains(block)) {
                    list.add(block);
                }
            }
        }
    }

    @EventTarget
    public void a(TickEvent event) {
        if ((aM_.gui.screen() instanceof DeathScreen) && aM_.player.deathTime >= 5) {
            aM_.player.respawn();
        }
        if (aM_.player == null || aM_.level == null) {
            return;
        }
        if (aM_.player.hasEffect(MobEffects.DARKNESS)) {
            aM_.player.removeEffect(MobEffects.DARKNESS);
        }
        if (aM_.options.keyShift.isDown() && aM_.options.keyJump.isDown()) {
            a();
        }
        for (Entity class_7260Var : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(256.0), e -> e instanceof Warden)) {
            if (class_7260Var instanceof Warden) {
                Warden warden = (Warden) class_7260Var;
                this.d.put(warden.blockPosition(), Integer.valueOf(aM_.player.tickCount + 100));
            }
        }
        this.d.values().removeIf(expire -> {
            return aM_.player.tickCount > expire.intValue();
        });
        if (this.n != null) {
            if (aM_.player.tickCount >= 20 && aM_.player.tickCount < 30) {
                aM_.player.connection.sendChat("/report " + this.n + " чит");
                this.n = null;
                return;
            }
            return;
        }
        if (aM_.player.tickCount < 5) {
            this.i = 0;
            this.c.clear();
            return;
        }
        if (ServerUtil.a.d() < 0) {
            if (aM_.player.tickCount % 100 == 0 && t() >= 0 && aM_.player.tickCount > 300) {
                aM_.player.connection.sendCommand("an" + t());
            }
            this.o = a.SAVE;
            return;
        }
        if (aM_.player.tickCount % 100 == 0 && E() && (this.k == null || !G())) {
            F();
        }
        if (aM_.player.hasEffect(MobEffects.GLOWING) && b(32.0d)) {
            e(false);
            return;
        }
        if (aM_.player.getFoodData().getFoodLevel() < 15 && !UseItemUtil.b() && !aM_.player.isUsingItem()) {
            UseItemUtil.a(stack -> stack.is(Items.GOLDEN_CARROT));
        }
        switch (this.o) {
            case SAVE:
                v();
                break;
            case TAKE:
                w();
                break;
            case COLLECTING:
                x();
                break;
            case ESCAPE:
                B();
                break;
        }
        if (!r() || aM_.player.tickCount % 15 != 0) {
            return;
        }
        C();
    }

    @EventTarget
    public void a(PacketEvent event) {
        if (event.c() && aM_.player != null && event.d() instanceof ClientboundUpdateMobEffectPacket effectPacket
                && effectPacket.getEffect().is(MobEffects.DARKNESS)
                && effectPacket.getEntityId() == aM_.player.getId()) {
            event.a(true);
            return;
        }
        if (event.c() && event.d() instanceof ClientboundDisguisedChatPacket message) {
            if (!message.message().getString().contains("Помянем. Вы погибли")) {
                return;
            }
                this.j = true;
                String text = message.message().getString();
                if (this.f.c().booleanValue() && aM_.player != null && text.contains("Вас убил")) {
                    StringBuilder effects = new StringBuilder();
                    for (MobEffectInstance effect : aM_.player.getActiveEffects()) {
                        effects.append(effect.getEffect().value().getDisplayName().getString()).append(StringUtils.a);
                    }
                    ChatUtil.a((Object) ("Эффекты при смерти: " + (effects.isEmpty() ? "нет" : effects.toString().trim())));
                    if (!aM_.player.hasEffect(MobEffects.GLOWING) && !a(2.0d)) {
                        this.n = text.split("Вас убил ")[1].split(",")[0].trim();
                    }
                }
        }
    }

    @EventTarget
    public void a(SoundEvent event) {
        String path = event.b().getIdentifier().getPath();
        if (aM_.player != null) {
            if (path.contains("warden.roar") || path.contains("warden.angry") || path.contains("warden.sonic")) {
                this.i = aM_.player.tickCount + 100;
            }
        }
    }

    @EventTarget
    public void a(InputEvent event) {
        if (aM_.player == null) {
            return;
        }
        if (!aM_.player.onGround() && !aM_.player.onClimbable()) {
            event.c(false);
        }
        if (r() && !UseItemUtil.b() && aM_.player.getMainHandItem().isEmpty() && !a(3.0d)) {
            int dir = ((float) (aM_.player.tickCount % 10)) <= MathUtil.a(3.0f, 8.0f) ? -1 : 1;
            event.a(dir);
            event.b(dir);
        }
    }

    private boolean r() {
        if (aM_.level.getBlockState(aM_.player.blockPosition()).is(BlockTags.CANDLES) || aM_.level.getBlockState(aM_.player.blockPosition().below()).is(BlockTags.CANDLES)) {
            return true;
        }
        return !A() && this.o == a.COLLECTING && E() && aM_.gui.screen() == null && !L() && !Delta.h().d().v().k().a() && s();
    }

    private boolean s() {
        AABB box = aM_.player.getBoundingBox().expandTowards(0.05000000009506496d, 0.0d, 0.05000000009506496d);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!aM_.level.getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private boolean a(double range) {
        for (BlockPos chest : Delta.h().d().t().i().q()) {
            if (aM_.player.distanceToSqr(Vec3.atCenterOf(chest)) <= range * range) {
                return true;
            }
        }
        return false;
    }

    private int t() {
        if (this.b.isEmpty()) {
            return -1;
        }
        return ((Integer) this.b.getFirst()).intValue();
    }

    private boolean u() {
        return t() >= 0 && t() == ServerUtil.a.d();
    }

    private void v() {
        if (t() >= 0 && ServerUtil.a.d() != t() && !ServerUtil.e() && aM_.player.tickCount % 5 == 0 && aM_.player.tickCount > 5) {
            aM_.player.connection.sendCommand("an" + t());
        }
        if (u()) {
            M();
            a(R(), true, a.TAKE);
        }
    }

    private void w() {
        if (this.j && this.b.size() > 1) {
            int i = this.h + 1;
            this.h = i;
            if (i >= this.b.size()) {
                this.h = 1;
            }
            this.j = false;
        }
        this.i = 0;
        this.c.clear();
        if (aM_.player.tickCount % 20 == 0) {
            StringBuilder missing = new StringBuilder("Собираем (возможно не хватает) -> ");
            if (Q() < 1) {
                missing.append("зелье невидимости, ");
            }
            if (InventoryUtil.a(Items.GOLDEN_CARROT) < 3) {
                missing.append("золотая морковь, ");
            }
            if (this.e.c().booleanValue() && a(this::f) < 0) {
                missing.append("зелье скорости ");
            }
            if (aM_.player.tickCount % InterfaceC0020Opcode.aN == 0 && !missing.isEmpty() && !O()) {
                aM_.player.clientSideCloseContainer();
            }
        }
        if (u()) {
            boolean potionOutsideHotbar = Q() > 0 && aHotbarPotion() < 0;
            if (!P() && !O() && potionOutsideHotbar) {
                movePotionToHotbar();
                return;
            }
            a(P() || O() || potionOutsideHotbar, false, a.COLLECTING);
        }
    }

    private int aHotbarPotion() {
        for (int i = 0; i < 9; i++) {
            if (e(aM_.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private void movePotionToHotbar() {
        if (UseItemUtil.b() || Delta.h().d().v().k().a() || !Delta.h().d().v().a().a().isEmpty()) {
            return;
        }
        if (aM_.player.tickCount % 10 != 0) {
            return;
        }
        int potion = a(this::e);
        if (potion < 9) {
            return;
        }
        int free = -1;
        for (int i = 0; i < 9; i++) {
            if (aM_.player.getInventory().getItem(i).isEmpty()) {
                free = i;
                break;
            }
        }
        if (free < 0) {
            free = aM_.player.getInventory().getSelectedSlot();
        }
        Delta.h().d().v().a().a(potion, free, 2);
    }

    private void x() {
        if (y()) {
            return;
        }
        Delta.h().d().t().aV().b(18);
        if (Delta.h().d().v().k().a()) {
            if (L()) {
                C();
                return;
            }
            return;
        }
        if (this.b.size() <= 1) {
            if (aM_.player.tickCount % 20 == 0) {
                ChatUtil.a((Object) "ОШИБКА -> .warden list пустой");
                return;
            }
            return;
        }
        if (this.h >= this.b.size()) {
            this.h = 1;
        }
        int target = this.b.get(this.h).intValue();
        if (ServerUtil.a.d() != target) {
            if (aM_.player.tickCount % 10 != 0 || aM_.player.tickCount <= 10) {
                return;
            }
            aM_.player.connection.sendCommand("an" + target);
            return;
        }
        if (aM_.player.tickCount > 5) {
            z();
        }
    }

    private int b(int base) {
        double d;
        double d2 = base;
        if (this.g.l("Низкий")) {
            d = 1.5d;
        } else {
            d = this.g.l("Высокий") ? 0.80000014538821d : 1.0d;
        }
        return (int) (d2 * d);
    }

    private boolean y() {
        boolean aggro = A();
        if ((aggro || D() > b(20) || aM_.player.getFoodData().getFoodLevel() < 8 || (this.c.values().stream().filter(count -> {
            return count.intValue() >= 2;
        }).count() >= 3 && aM_.player.tickCount % 30 == 0)) && aM_.player.tickCount > 100) {
            if (aggro) {
                this.j = true;
            }
            this.o = a.ESCAPE;
            return true;
        }
        if (!ServerUtil.e() && D() > b(8)) {
            this.o = a.ESCAPE;
            return true;
        }
        int pvpTime = ServerUtil.f();
        if (pvpTime >= 0 && pvpTime < 7 && !b(14.0d) && D() > b(7)) {
            this.o = a.ESCAPE;
            return true;
        }
        return false;
    }

    private void z() {
        if (UseItemUtil.b()) {
            return;
        }
        MobEffectInstance invis = aM_.player.getEffect(MobEffects.INVISIBILITY);
        boolean ready = aM_.player.hasEffect(MobEffects.GLOWING) || (invis != null && invis.getDuration() >= 400);
        if (!ready && invis == null && Q() < 1 && aM_.player.tickCount % 5 == 0 && !ServerUtil.e()) {
            this.o = a.ESCAPE;
            return;
        }
        if (!ready) {
            K();
            if (UseItemUtil.b()) {
                return;
            }
        }
        if (!E()) {
            if (aM_.player.tickCount % 50 == 0) {
                aM_.player.connection.sendCommand("home");
            }
        } else if (ready) {
            boolean needSpeed = this.e.c().booleanValue() && aM_.player.getEffect(MobEffects.SPEED) == null;
            if (!needSpeed) {
                H();
            } else if (!UseItemUtil.a(this::f) && !UseItemUtil.b()) {
                H();
            }
        }
    }

    private boolean A() {
        if (aM_.player.tickCount < this.i) {
            for (Entity entity : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(48.0d), e -> e instanceof Warden)) {
                if (entity instanceof Warden) {
                    Warden warden = (Warden) entity;
                    double distSq = aM_.player.distanceToSqr(warden);
                    if (distSq < 900.0d && b(warden) && (distSq < 16.0d || a(warden))) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private boolean a(Warden warden) {
        return ((aM_.player.getX() - warden.getX()) * (warden.getX() - warden.xo)) + ((aM_.player.getZ() - warden.getZ()) * (warden.getZ() - warden.zo)) > 0.010000003841705648d;
    }

    private boolean b(Warden warden) {
        double yawToMe = Math.toDegrees(Math.atan2(-(aM_.player.getX() - warden.getX()), aM_.player.getZ() - warden.getZ()));
        return Math.abs(((((((double) warden.yBodyRot) - yawToMe) % 360.0d) + 540.0d) % 360.0d) - 180.0d) < 10.0d;
    }

    private void B() {
        if (u()) {
            this.o = a.SAVE;
            return;
        }
        if (A() && ServerUtil.e()) {
            e(true);
            return;
        }
        BlockPos near = J();
        if ((aM_.gui.screen() instanceof ContainerScreen) || (near != null && Delta.h().d().t().i().a(near) < 0 && aM_.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(near)) <= 16.0d)) {
            H();
            return;
        }
        if (ServerUtil.e()) {
            if (D() >= 23 || b(2.0d) || ServerUtil.f() <= 16 || near == null) {
                e(true);
                return;
            } else {
                H();
                return;
            }
        }
        this.o = a.SAVE;
    }

    private void e(boolean warden) {
        N();
        M();
        BlockPos best = null;
        double bestScore = -1.0d;
        int y = aM_.player.blockPosition().getY();
        for (int angle = 0; angle < 360; angle += 30) {
            int x = c((int) (aM_.player.getX() + (Math.cos(Math.toRadians(angle)) * 25.0d)));
            int z = d((int) (aM_.player.getZ() + (Math.sin(Math.toRadians(angle)) * 25.0d)));
            double score = a(x, z, warden);
            if (score > bestScore) {
                bestScore = score;
                best = new BlockPos(x, y, z);
            }
        }
        a(best);
    }

    private void a(BlockPos spot) {
        if (spot != null) {
            if (aM_.player.tickCount % 10 == 0 || (!L() && aM_.player.tickCount % 5 == 0)) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(new BlockPos(c(spot.getX()), spot.getY(), d(spot.getZ()))));
            }
        }
    }

    private void C() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    private double a(int x, int z, boolean warden) {
        double min = 1.7976922776554316E308d;
        for (Entity class_746Var : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(1024.0d), e -> true)) {
            if (class_746Var != aM_.player && ((class_746Var instanceof Player) || (warden && (class_746Var instanceof Warden)))) {
                min = Math.min(min, Math.hypot(class_746Var.getX() - ((double) x), class_746Var.getZ() - ((double) z)));
            }
        }
        return min;
    }

    private int D() {
        int n = 0;
        for (ItemStack stack : aM_.player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private boolean E() {
        return aM_.level.dimension().identifier().toString().equals("minecraft:overworld") && aM_.player.getX() <= -1921.0d && aM_.player.getX() >= -2070.0d && aM_.player.getZ() <= -1929.0d && aM_.player.getZ() >= -2076.0d;
    }

    private void F() {
        this.k = new AABB(-2070.0d, aM_.player.blockPosition().getY(), -2076.0d, -1921.0d, aM_.player.blockPosition().getY(), -1929.0d);
    }

    private boolean G() {
        return this.k != null && aM_.player.getX() >= this.k.minX && aM_.player.getX() <= this.k.maxX && aM_.player.getZ() >= this.k.minZ && aM_.player.getZ() <= this.k.maxZ;
    }

    private int c(int x) {
        return this.k == null ? x : (int) Math.max(this.k.minX + 10.0d, Math.min(this.k.maxX - 10.0d, x));
    }

    private int d(int z) {
        return this.k == null ? z : (int) Math.max(this.k.minZ + 10.0d, Math.min(this.k.maxZ - 10.0d, z));
    }

    private void H() {
        Screen class_437Var = aM_.gui.screen();
        if (class_437Var instanceof ContainerScreen) {
            ContainerScreen screen = (ContainerScreen) class_437Var;
            a(screen);
            return;
        }
        BlockPos pick = J();
        if (pick == null) {
            pick = I();
        }
        boolean stay = (pick == null || this.l == null || pick.equals(this.l) || Delta.h().d().t().i().a(this.l) <= 25000) ? false : true;
        if (!stay) {
            this.m.b();
        }
        if (!stay || this.m.a(1000L)) {
            this.l = pick;
        }
        BlockPos target = this.l;
        if (target == null && aM_.player.tickCount % 40 == 0) {
            this.o = a.ESCAPE;
            this.j = true;
        }
        long remaining = Delta.h().d().t().i().a(target);
        if (target != null && remaining > 1000 && a(target, 7.0d)) {
            BlockPos spot = c(target);
            if (spot != null) {
                if (aM_.player.distanceToSqr(Vec3.atCenterOf(spot)) > 2.0d) {
                    a(spot);
                    return;
                } else {
                    C();
                    return;
                }
            }
            return;
        }
        if (remaining > 2000) {
            BlockPos waitSpot = c(target);
            a(waitSpot != null ? waitSpot : b(target));
            return;
        }
        double distSq = aM_.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(target));
        if (distSq <= 20.0d) {
            if (this.c.getOrDefault(target, 0).intValue() < 8) {
                if (a(target, remaining >= 0 ? 6 : 1)) {
                    this.c.merge(target, 1, (v0, v1) -> {
                        return Integer.sum(v0, v1);
                    });
                    return;
                }
                return;
            }
            return;
        }
        if (distSq > 10.0d) {
            M();
        }
        a(c(target));
    }

    private BlockPos b(BlockPos chest) {
        double angle = ((double) (aM_.player.tickCount / 40)) * 2.4000011930854526d;
        return new BlockPos(c(chest.getX() + ((int) (Math.cos(angle) * 10.0d))), chest.getY(), d(chest.getZ() + ((int) (Math.sin(angle) * 10.0d))));
    }

    private BlockPos I() {
        WardenESP esp = Delta.h().d().t().i();
        BlockPos best = null;
        long bestMs = 45000;
        for (BlockPos chest : esp.q()) {
            long remaining = esp.a(chest);
            if (remaining >= 0 && remaining < bestMs && f(chest) && !d(chest) && !e(chest)) {
                bestMs = remaining;
                best = chest;
            }
        }
        return best;
    }

    private BlockPos J() {
        WardenESP esp = Delta.h().d().t().i();
        BlockPos best = null;
        int bestTier = 99;
        double bestSq = 1.7976922776554316E308d;
        for (BlockPos chest : esp.q()) {
            if (f(chest) && !e(chest)) {
                double distSq = aM_.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(chest));
                long remaining = esp.a(chest);
                if (!d(chest) || (remaining < 0 && distSq <= 16.0d)) {
                    if (remaining >= 0 || this.c.getOrDefault(chest, 0).intValue() < 8) {
                        int tier = -1;
                        if (remaining < 0 && distSq <= 25.0d) {
                            tier = 0;
                        } else if (remaining >= 0 && remaining <= 5000 && distSq <= 144.0d) {
                            tier = 1;
                        } else if (remaining < 0 && distSq <= 144.0d) {
                            tier = 2;
                        } else if (remaining >= 0 && remaining <= 15000 && distSq <= 625.0d) {
                            tier = 3;
                        } else if (remaining < 0) {
                            tier = 4;
                        }
                        if (tier < 0) {
                            continue;
                        }
                        double up = Vec3.atCenterOf(chest).y - aM_.player.getEyeY();
                        double dx = (((double) chest.getX()) + 0.5d) - aM_.player.getX();
                        double dz = (((double) chest.getZ()) + 0.5d) - aM_.player.getZ();
                        double weightedSq = (dx * dx) + (dz * dz) + (((double) (up > 0.0d ? 2 : 1)) * up * up);
                        if (tier < bestTier || (tier == bestTier && weightedSq < bestSq)) {
                            bestTier = tier;
                            bestSq = weightedSq;
                            best = chest;
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean b(double range) {
        for (Entity _e : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(range + 16.0d), e -> true)) {
            if (!(_e instanceof Player player)) continue;
            if (player != aM_.player && aM_.player.distanceToSqr(player) < range * range) {
                return true;
            }
        }
        return false;
    }

    private void a(ContainerScreen screen) {
        if (L()) {
            C();
            return;
        }
        if (aM_.player.tickCount % 2 != 0) {
            return;
        }
        Slot slot = a(screen, false, stack -> {
            return (stack.isEmpty() || b(stack)) ? false : true;
        });
        if (slot == null) {
            N();
        } else {
            a(screen, slot, 0, ContainerInput.QUICK_MOVE);
        }
    }

    private final Map<BlockPos, BlockPos> spotCache = new HashMap();
    private int spotCacheTick;

    private BlockPos c(BlockPos chest) {
        if (aM_.player.tickCount - this.spotCacheTick > 100 || this.spotCache.size() > 400) {
            this.spotCache.clear();
            this.spotCacheTick = aM_.player.tickCount;
        }
        BlockPos cached = this.spotCache.get(chest);
        if (cached != null) {
            if (aM_.level.getBlockState(cached).isAir() && aM_.level.getBlockState(cached.above()).isAir()) {
                return cached;
            }
            this.spotCache.remove(chest);
        }
        BlockPos found = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && found == null) {
                    BlockPos side = chest.offset(dx, 0, dz);
                    if (aM_.level.getBlockState(side).isAir() && aM_.level.getBlockState(side.above()).isAir() && !aM_.level.getBlockState(side.below()).isAir() && a(side, chest)) {
                        found = side;
                    }
                }
            }
        }
        if (found == null && aM_.level.getBlockState(chest.above()).isAir() && aM_.level.getBlockState(chest.above().above()).isAir() && a(chest.above(), chest)) {
            found = chest.above();
        }
        if (found != null) {
            this.spotCache.put(chest, found);
        }
        return found;
    }

    private boolean a(BlockPos from, BlockPos chest) {
        return a(Vec3.atCenterOf(from).add(0.0d, ((double) aM_.player.getEyeHeight(aM_.player.getPose())) - 0.5d, 0.0d), chest) != null;
    }

    private Vec3 a(Vec3 eye, BlockPos chest) {
        Vec3 center = Vec3.atCenterOf(chest);
        Vec3 best = null;
        double bestSq = Double.MAX_VALUE;
        for (double dx = -0.3999999563044224d; dx <= 0.41000000193542635d; dx += 0.4000000009895358d) {
            for (double dy = -0.3999999563044224d; dy <= 0.41000000193542635d; dy += 0.4000000009895358d) {
                for (double dz = -0.3999999563044224d; dz <= 0.41000000193542635d; dz += 0.4000000009895358d) {
                    Vec3 point = center.add(dx, dy, dz);
                    double sq = point.distanceToSqr(center);
                    if (sq < bestSq && aM_.level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, aM_.player)).getBlockPos().equals(chest)) {
                        bestSq = sq;
                        best = point;
                    }
                }
            }
        }
        return best;
    }

    private boolean d(BlockPos pos) {
        for (BlockPos warden : this.d.keySet()) {
            if (warden.distSqr(pos) < 25.0d) {
                return true;
            }
        }
        return false;
    }

    private boolean e(BlockPos pos) {
        for (Entity _e : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(160.0d), e -> true)) {
            if (!(_e instanceof Player player)) continue;
            if (player != aM_.player && player.position().distanceToSqr(Vec3.atCenterOf(pos)) < 20.0d && Stream.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).anyMatch(slot -> {
                return Interface.isHumanoidArmor(player.getItemBySlot(slot));
            })) {
                return true;
            }
        }
        return false;
    }

    private boolean a(BlockPos pos, double range) {
        for (Entity _e : aM_.level.getEntities(aM_.player, aM_.player.getBoundingBox().inflate(range + 16.0d), e -> true)) {
            if (!(_e instanceof Player player)) continue;
            if (player != aM_.player && player.position().distanceToSqr(Vec3.atCenterOf(pos)) < range * range) {
                return true;
            }
        }
        return false;
    }

    private boolean f(BlockPos chest) {
        return c(chest) != null;
    }

    private void K() {
        if (aM_.player.tickCount > 20) {
            UseItemUtil.a(this::e);
        }
    }

    private int a(Predicate<ItemStack> match) {
        for (int i = 0; i < 36; i++) {
            if (match.test(aM_.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private void a(boolean active, boolean hopper, a next) {
        if (!active) {
            if (N()) {
                this.o = next;
                return;
            }
            return;
        }
        Screen class_437Var = aM_.gui.screen();
        if (class_437Var instanceof ContainerScreen) {
            ContainerScreen screen = (ContainerScreen) class_437Var;
            if (!hopper) {
                c(screen);
                return;
            } else {
                b(screen);
                return;
            }
        }
        a(f(hopper), 2);
    }

    private boolean a(BlockPos chest, int rate) {
        Vec3 eye;
        Vec3 aim;
        if (chest == null || (aM_.gui.screen() instanceof ContainerScreen) || (aim = a((eye = aM_.player.getEyePosition()), chest)) == null) {
            return false;
        }
        Rotation target = Rotation.a(eye, aim);
        float t = aM_.player.tickCount + aM_.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float sw = (float) (((Math.sin(t * 0.31f) * 0.5d) + (Math.sin((t * 0.73f) + 1.1f) * 0.3000000317022817d) + (Math.sin((t * 1.7f) + 2.6f) * 0.1999999860971588d)) * 1.5d);
        Delta.h().d().k().a(new Rotation(target.c() + sw, MathUtil.b(target.d() + (sw / 4.0f), -90.0f, 90.0f)), 35.0f, 1, 1);
        if (aM_.player.tickCount % rate != 0 || Rotation.b().a(target) > 5.0d) {
            return false;
        }
        BlockHitResult hit = aM_.level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, aM_.player));
        if (!hit.getBlockPos().equals(chest)) {
            return false;
        }
        aM_.gameMode.useItemOn(aM_.player, InteractionHand.MAIN_HAND, hit);


        return true;
    }

    private boolean L() {
        return aM_.player.getDeltaMovement().horizontalDistanceSqr() > 0.002500001077917312d;
    }

    private void M() {
        if (UseItemUtil.b() || Delta.h().d().v().k().a()) {
            return;
        }
        if (aM_.player.getMainHandItem().isEmpty()) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (aM_.player.getInventory().getItem(i).isEmpty()) {
                aM_.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
        for (int i2 = 9; i2 < 36; i2++) {
            if (aM_.player.getInventory().getItem(i2).isEmpty()) {
                if (aM_.player.tickCount % 10 >= 2 && L()) {
                    C();
                }
                if (aM_.player.tickCount % 10 == 4) {
                    Delta.h().d().v().a().a(aM_.player.getInventory().getSelectedSlot(), i2, 1);
                    return;
                }
                return;
            }
        }
    }

    private boolean N() {
        if ((aM_.gui.screen() instanceof ContainerScreen) && aM_.player.tickCount % 2 == 0) {
            aM_.player.clientSideCloseContainer();
        }
        return !(aM_.gui.screen() instanceof ContainerScreen);
    }

    private void b(ContainerScreen screen) {
        if (aM_.player.tickCount % 2 != 0) {
            return;
        }
        boolean keepPotion = false;
        boolean keepCarrot = false;
        int moved = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (moved < 4) {
                ItemStack stack = slot.getItem();
                if (slot.container == aM_.player.getInventory() && !stack.isEmpty() && (!this.e.c().booleanValue() || !f(stack))) {
                    if (!keepPotion && e(stack)) {
                        keepPotion = true;
                    } else if (keepCarrot || !stack.is(Items.GOLDEN_CARROT)) {
                        a(screen, slot, 0, ContainerInput.QUICK_MOVE);
                        moved++;
                    } else {
                        keepCarrot = true;
                    }
                }
            } else {
                return;
            }
        }
    }

    private void c(ContainerScreen screen) {
        if (aM_.player.tickCount % 2 != 0) {
            return;
        }
        ItemStack cursor = screen.getMenu().getCarried();
        Predicate<ItemStack> same = s -> {
            return s.isEmpty() || ItemStack.isSameItemSameComponents(s, cursor);
        };
        if (!cursor.isEmpty()) {
            if (!a(cursor)) {
                a(screen, a(screen, false, same), 0, ContainerInput.PICKUP);
                return;
            } else {
                a(screen, a(screen, true, same), 1, ContainerInput.PICKUP);
                return;
            }
        }
        a(screen, a(screen, false, this::a), 0, ContainerInput.PICKUP);
    }

    private Slot a(ContainerScreen screen, boolean player, Predicate<ItemStack> match) {
        for (Slot slot : screen.getMenu().slots) {
            if ((slot.container == aM_.player.getInventory()) == player && match.test(slot.getItem())) {
                return slot;
            }
        }
        return null;
    }

    private void a(ContainerScreen screen, Slot slot, int button, ContainerInput type) {
        if (slot != null) {
            aM_.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, button, type, aM_.player);
        }
    }

    private boolean O() {
        ContainerScreen class_476Var = (ContainerScreen) aM_.gui.screen();
        if (class_476Var instanceof ContainerScreen) {
            ContainerScreen screen = class_476Var;
            if (!screen.getMenu().getCarried().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean a(ItemStack stack) {
        return !stack.isEmpty() && ((e(stack) && Q() < 1) || ((stack.is(Items.GOLDEN_CARROT) && InventoryUtil.a(Items.GOLDEN_CARROT) < 3) || (this.e.c().booleanValue() && f(stack) && a(this::f) < 0)));
    }

    private boolean P() {
        return Q() < 1 || InventoryUtil.a(Items.GOLDEN_CARROT) < 3 || (this.e.c().booleanValue() && a(this::f) < 0);
    }

    private int Q() {
        int total = 0;
        for (ItemStack stack : aM_.player.getInventory().getNonEquipmentItems()) {
            if (e(stack)) {
                total++;
            }
        }
        return total;
    }

    private boolean b(ItemStack stack) {
        if (this.g.l("Низкий")) {
            return false;
        }
        return d(stack) || (this.g.l("Высокий") && c(stack));
    }

    private boolean c(ItemStack stack) {
        Item item = stack.getItem();
        return (item instanceof ArrowItem) ||  false || (item instanceof AxeItem) || stack.is(Items.CHORUS_FRUIT) || stack.is(Items.DISC_FRAGMENT_5) || stack.is(Items.NAUTILUS_SHELL) || stack.is(Items.BOOKSHELF) || stack.is(Items.COOKED_MUTTON) || stack.is(Items.SKELETON_SPAWN_EGG) || stack.is(Items.CREEPER_SPAWN_EGG) || stack.is(Items.ZOMBIE_SPAWN_EGG) || stack.is(Items.VINDICATOR_SPAWN_EGG) || stack.is(Items.PIGLIN_SPAWN_EGG) || stack.is(Items.FIRE_CHARGE) || stack.is(Items.LEATHER) || stack.is(Items.SHULKER_SHELL) || stack.is(Items.EXPERIENCE_BOTTLE) || stack.is(Items.WITHER_ROSE) || stack.is(Items.EMERALD) || stack.is(Items.SUGAR) || a(stack, "potion-popper") || stack.has(DataComponents.JUKEBOX_PLAYABLE) || stack.is(Items.GHAST_TEAR) || stack.is(Items.DRAGON_BREATH) || stack.is(Items.VEX_SPAWN_EGG) || stack.is(Items.ENDERMITE_SPAWN_EGG) || stack.is(Items.CAT_SPAWN_EGG) || stack.is(Items.ENCHANTING_TABLE) || stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS);
    }

    private boolean d(ItemStack stack) {
        Item item = stack.getItem();
        return (item instanceof ShovelItem) || (item instanceof AxeItem) || (item instanceof BannerItem) || ((item instanceof SmithingTemplateItem) && !stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) || stack.is(Items.BLAZE_ROD) || stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.TRIDENT) || stack.is(Items.NAME_TAG) || stack.is(Items.SCULK) || stack.is(Items.SCULK_SENSOR) || stack.is(Items.ENDER_CHEST) || stack.is(Items.REINFORCED_DEEPSLATE) || stack.is(Items.PUFFERFISH) || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.FERMENTED_SPIDER_EYE) || stack.is(Items.ANVIL) || stack.is(Items.COOKED_PORKCHOP);
    }

    private boolean a(ItemStack stack, String id) {
        CustomData data = (CustomData) stack.get(DataComponents.CUSTOM_DATA);
        return data != null && id.equals(data.copyTag().getCompoundOrEmpty("PublicBukkitValues").getStringOr("minecraft:ftid", ""));
    }

    private boolean e(ItemStack stack) {
        Holder<Potion> potion = (Holder) ((PotionContents) stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).potion().orElse(null);
        return potion != null && (potion.equals(Potions.INVISIBILITY) || potion.equals(Potions.LONG_INVISIBILITY));
    }

    private boolean f(ItemStack stack) {
        if (stack.is(Items.POTION)) {
            PotionContents contents = (PotionContents) stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            Holder<Potion> potion = (Holder<Potion>) contents.potion().orElse(null);
            if (potion != null && (potion.equals(Potions.SWIFTNESS) || potion.equals(Potions.LONG_SWIFTNESS) || potion.equals(Potions.STRONG_SWIFTNESS))) {
                return true;
            }
            for (MobEffectInstance effect : contents.customEffects()) {
                if (effect.getEffect().equals(MobEffects.SPEED)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private BlockPos f(boolean hopper) {
        BlockPos origin = aM_.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (aM_.level.getBlockState(pos).is(Blocks.CHEST) && g((BlockPos) pos) == hopper && aM_.level.getBlockState(pos.above()).isAir() && aM_.level.clip(new ClipContext(aM_.player.getEyePosition(), Vec3.atCenterOf(pos), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, aM_.player)).getBlockPos().equals(pos)) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    private boolean g(BlockPos pos) {
        if (aM_.level.getBlockState(pos.below()).is(Blocks.HOPPER)) {
            return true;
        }
        BlockState state = aM_.level.getBlockState(pos);
        if (state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos partner = pos.relative(dir);
                BlockState ps = aM_.level.getBlockState(partner);
                if (ps.is(Blocks.CHEST) && ps.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE && ps.getValue(BlockStateProperties.CHEST_TYPE) != state.getValue(BlockStateProperties.CHEST_TYPE) && ps.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING) && aM_.level.getBlockState(partner.below()).is(Blocks.HOPPER)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean R() {
        boolean keepPotion = false;
        boolean keepCarrot = false;
        for (ItemStack stack : aM_.player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && (!this.e.c().booleanValue() || !f(stack))) {
                if (!keepPotion && e(stack)) {
                    keepPotion = true;
                } else {
                    if (keepCarrot || !stack.is(Items.GOLDEN_CARROT)) {
                        return true;
                    }
                    keepCarrot = true;
                }
            }
        }
        return false;
    }
}








