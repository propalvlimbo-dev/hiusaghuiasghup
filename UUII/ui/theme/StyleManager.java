package wtf.expensive.client.ui.theme;

import wtf.expensive.client.util.render.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class StyleManager {
    public final List<Style> styles = new ArrayList<>();
    private Style currentStyle;

    public void init() {
        styles.clear();
        styles.add(new Style("Пурпурно фиолетовый", ColorUtil.fromHex("#FCFC36"), ColorUtil.fromHex("#5D00B2")));
        styles.add(new Style("Лавандово белый", ColorUtil.fromHex("#F4ECFF"), ColorUtil.fromHex("#765AA5")));
        styles.add(new Style("Разно цветный", true, ColorUtil.fromHex("#000000"), ColorUtil.fromHex("#FFFFFF")));
        styles.add(new Style("Лазурное небо", ColorUtil.fromHex("#07338A"), ColorUtil.fromHex("#0078FF")));
        styles.add(new Style("Лунный зефир", ColorUtil.fromHex("#E3C3FF"), ColorUtil.fromHex("#67FFEC")));
        styles.add(new Style("Искорка ветра", ColorUtil.fromHex("#FFC854"), ColorUtil.fromHex("#4288FF")));
        styles.add(new Style("Радужное мерцание", ColorUtil.fromHex("#FF00AA"), ColorUtil.fromHex("#AA00FF")));
        styles.add(new Style("Сумеречный огонек", ColorUtil.fromHex("#FF5C7D"), ColorUtil.fromHex("#5CFF9E")));
        styles.add(new Style("Золотой феникс", ColorUtil.fromHex("#FFC300"), ColorUtil.fromHex("#FF5800")));
        styles.add(new Style("Ледяной сказ", ColorUtil.fromHex("#B7EFFF"), ColorUtil.fromHex("#FFAAFF")));
        styles.add(new Style("Магический рассвет", ColorUtil.fromHex("#FF007F"), ColorUtil.fromHex("#00FFB7")));
        styles.add(new Style("Темный аметист", ColorUtil.fromHex("#42275A"), ColorUtil.fromHex("#734B6D")));
        styles.add(new Style("Морская волна", ColorUtil.fromHex("#343838"), ColorUtil.fromHex("#005F6B")));
        styles.add(new Style("Ночной шифер", ColorUtil.fromHex("#2C3E50"), ColorUtil.fromHex("#FD746C")));
        styles.add(new Style("Конфетный", ColorUtil.fromHex("#76ACD7"), ColorUtil.fromHex("#F15FE9")));
        styles.add(new Style("Кровавый", ColorUtil.fromHex("#FD3A3A"), ColorUtil.fromHex("#3A3A3A")));
        styles.add(new Style("Алый огонь", ColorUtil.fromHex("#8B8DF6"), ColorUtil.fromHex("#E60101")));
        styles.add(new Style("Лунно белый", ColorUtil.fromHex("#F4ECFF"), ColorUtil.fromHex("#76BEDF")));
        styles.add(new Style("Коралловый", ColorUtil.fromHex("#FF6347"), ColorUtil.fromHex("#0044FF")));
        styles.add(new Style("Черный океан", ColorUtil.fromHex("#373B44"), ColorUtil.fromHex("#4286F4")));
        styles.add(new Style("Свой цвет", ColorUtil.fromHex("#765AA5"), ColorUtil.fromHex("#F4ECFF")));
        currentStyle = styles.getFirst();
    }

    public void setCurrentStyle(Style style) {
        this.currentStyle = style;
    }

    public Style getCurrentStyle() {
        return currentStyle;
    }

    public int getColor(int index) {
        return currentStyle == null ? -1 : currentStyle.getColor(index);
    }
}
