package com.vendor_service.common.constants;

public final class ApiRoutes {

    private ApiRoutes() {}

    private static final String BASE = "/api";

    public static final class Vendor {
        public static final String PROFILE = BASE + "/vendor/profile";
        private Vendor() {}
    }

    public static final class Store {
        public static final String BASE = ApiRoutes.BASE + "/stores";
        public static final String BY_ID = BASE + "/{id}";
        public static final String TIMINGS = BY_ID + "/timings";
        public static final String HOLIDAYS = BY_ID + "/holidays";
        private Store() {}
    }

    public static final class Menu {
        public static final String CATEGORIES = BASE + "/categories";
        public static final String ITEMS = BASE + "/menu-items";
        private Menu() {}
    }

    public static final class Analytics {
        public static final String BASE = ApiRoutes.BASE + "/analytics";
        private Analytics() {}
    }

    public static final class Dashboard {
        public static final String BASE = ApiRoutes.BASE + "/dashboard";
        private Dashboard() {}
    }
}