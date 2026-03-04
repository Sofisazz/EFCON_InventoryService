package com.example.inventoryservice.map;

import com.example.inventoryservice.enums.Categories;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CategoryMap {

    private final Map<Categories, List<String>> categoryMap = new HashMap<>();

    CategoryMap() {
        categoryMap.put(Categories.DAIRY_AND_EGGS, List.of("молок", "сыр", "йогурт", "кефир", "творог", "сметан", "сливок", "масло", "яиц", "яичн", "ряженк", "простокваш"));
        categoryMap.put(Categories.MEAT, List.of("говядин", "свинин", "баранин", "телятин", "фарш", "стейк", "ребрышк", "грудинк", "вырезк"));
        categoryMap.put(Categories.PROCESSED_MEAT, List.of("колбас", "сосиск", "сарделек", "ветчин", "карбонат", "бекон", "паштет", "деликатес", "копченост"));
        categoryMap.put(Categories.SEAFOOD, List.of("рыб", "тунец", "кревет", "миди", "устриц", "кальмар", "осьминог", "краб", "икр", "сельд", "лосос", "горбуш", "минтай", "треск", "скумбр"));
        categoryMap.put(Categories.FRESH_PRODUCE, List.of("овощ", "фрукт", "ягод", "картоф", "капуст", "морков", "лук", "чеснок", "помидор", "огурец", "яблок", "банан", "апельсин", "лимон", "зелень", "укроп", "петрушк", "салат", "гриб"));
        categoryMap.put(Categories.FROZEN_FOODS, List.of("заморож", "пельмен", "вареник", "блинчик", "морожен", "смесь", "полуфабрикат"));
        categoryMap.put(Categories.CANNED_GOODS, List.of("консерв", "тушенк", "кукуруз", "горошек", "фасоль", "шпрот", "сайра"));
        categoryMap.put(Categories.GRAINS_AND_PASTA, List.of("макарон", "круп", "рис", "гречк", "овсян", "пшен", "перловк", "манн", "спагетт", "лапш", "вермишел", "хлопья"));
        categoryMap.put(Categories.SWEETS, List.of("шоколад", "конфет", "печен", "торт", "пирожн", "вафл", "пряник", "мармелад", "зефир", "пастил", "сахар", "мед", "варень", "джем", "мюсли", "батончик"));
        categoryMap.put(Categories.BAKERY, List.of("хлеб", "булк", "сдоб", "батон", "багет", "лаваш", "пирожк", "круассан", "сухар", "тост", "лепешк"));
        categoryMap.put(Categories.CONDIMENTS, List.of("соус", "кетчуп", "майонез", "маринад", "паста", "томат", "горчиц", "хрен", "уксус", "приправ", "специ", "масло растит", "укроп суш", "базилик"));
        categoryMap.put(Categories.SNACKS, List.of("чипс", "сухарик", "снек", "попкорн", "орех", "семечк", "сушен", "вялен", "фисташк", "миндаль", "арахис"));
        categoryMap.put(Categories.BABY_FOOD, List.of("детск питан", "пюре детск", "смесь детск", "каш детск", "для детей", "для младенц"));
        categoryMap.put(Categories.ALCOHOL, List.of("пиво", "вин", "коньяк", "ликер", "шампан", "водк", "виски", "ром", "джин", "сидр", "медовух"));
        categoryMap.put(Categories.NON_ALCOHOLIC_DRINKS, List.of("сок", "вода", "чай", "кофе", "лимонад", "газиров", "энергетик", "квас", "морс", "компот", "коктейль", "какао", "минерал", "cola", "sprit", "up", "mirid"));
        categoryMap.put(Categories.READY_MEALS, List.of("готов", "блюдо", "пицца", "суп", "борщ", "плов", "гарнир", "салат готов", "шаурм"));
    }

    public Categories defineCategory(String name){
        String lower = name.toLowerCase();

        for(Map.Entry<Categories, List<String>> entry : categoryMap.entrySet()) {
            for(String def : entry.getValue()) {
                if (lower.contains(def)) {
                    return entry.getKey();
                }
            }
        }

        return Categories.OTHER;
    }
}