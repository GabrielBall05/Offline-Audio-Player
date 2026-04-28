package com.example.offlineplayer.util

interface SortOption {
    val label: String
}

enum class MediaSortOrder(override val label: String) : SortOption {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    CREATOR_ASC ("Creator (A-Z)"),
    CREATOR_DESC("Creator (Z-A)"),
    DURATION_ASC("Duration (Low-High)"),
    DURATION_DESC("Duration (High-Low)"),
    DATE_ADDED_ASC("Date Added (Most Recent)"),
    DATE_ADDED_DESC("Date Added (Least Recent)")
}

enum class PlaylistSortOrder(override val label: String): SortOption {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    ITEM_COUNT_ASC("Items (Low-High)"),
    ITEM_COUNT_DESC("Items (High-Low)"),
    DATE_CREATED_ASC("Date Created (Most Recent)"),
    DATE_CREATED_DESC("Date Created (Least Recent)")
}