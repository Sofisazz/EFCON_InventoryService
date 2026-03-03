package com.example.inventoryservice.enums;

public enum Categories {
    DAIRY_AND_EGGS("Молочные продукты и яйца"),
    MEAT("Мясо"),
    PROCESSED_MEAT("Колбасы и деликатесы"),
    SEAFOOD("Рыба и морепродукты"),
    FRESH_PRODUCE("Свежие овощи и фрукты"),
    FROZEN_FOODS("Замороженные продукты"),
    CANNED_GOODS("Консервы"),
    GRAINS_AND_PASTA("Крупы и макароны"),
    BAKERY("Выпечка"),
    CONDIMENTS("Соусы, маринады, пасты"),
    SNACKS("Закуски"),
    BABY_FOOD("Детское питание"),
    ALCOHOL("Алкогольные напитки"),
    NON_ALCOHOLIC_DRINKS("Безалкогольные напитки"),
    READY_MEALS("Готовые блюда"),
    SWEETS("Сладости"),
    OTHER("Прочее");

    private String categoryDescription;

    Categories(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

}
