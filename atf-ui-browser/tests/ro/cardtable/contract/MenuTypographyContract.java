package ro.cardtable.contract;

import ro.cardtable.ui.menu.MenuTypography;

public final class MenuTypographyContract {
    public static void main(String[] args) {
        var entries = MenuTypography.browserRoles().entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue()).sorted().toList();
        System.out.println("{" + String.join(",", entries) + "}");
    }
}
