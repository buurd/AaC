workspace "My System" "My System Description" {
    model {
        customer = person "Customer" "A customer of the webshop." {
            tags "Person" "Logical"
        }
        productManager = person "Product Manager" "A manager of products" {
            tags "Person" "Logical"
        }
        warehouseStaff = person "Warehouse Staff" "Staff managing inventory." {
            tags "Person" "Logical"
        }

        gateway = softwareSystem "API Gateway / Reverse Proxy" "Entry point for all traffic (HTTPS)." {
            tags "Software System" "Infrastructure"
            reverseProxy = container "Nginx" "Handles SSL termination and routing." {
                tags "Container" "Infrastructure"
            }
        }

        webshop = softwareSystem "Webshop" "The webshop system." {
            tags "Software System" "Logical"
            webServer = container "Webshop WebServer" "The web server." {
                tags "Container" "Web Server" "Logical"
                productController = component "ProductController" "Handles product listing." {
                    tags "Component"
                }
                productSyncController = component "ProductSyncController" "Handles product synchronization API." {
                    tags "Component"
                }
                stockSyncController = component "StockSyncController" "Handles stock synchronization API." {
                    tags "Component"
                }
                productRepository = component "ProductRepository" "Handles data access for products." {
                    tags "Component" "Repository"
                }
            }
            database = container "Webshop Database" "The database." {
                tags "Container" "Database" "Logical"
            }
        }

        productManagementSystem = softwareSystem "Product Management System" "The Product Management System" {
            tags "Software System" "Logical"
            pmWebServer = container "PM WebServer" "The web server." {
                tags "Container" "Web Server" "Logical"
                pmProductController = component "ProductController" "Handles product management (CRUD)." {
                    tags "Component"
                }
                pmProductService = component "ProductService" "Handles business logic and sync." {
                    tags "Component" "Service"
                }
                pmProductRepository = component "ProductRepository" "Handles data access for products." {
                    tags "Component" "Repository"
                }
            }
            pmDatabase = container "PM Database" "The database." {
                tags "Container" "Database" "Logical"
            }
        }

        warehouseService = softwareSystem "Warehouse Service" "The warehouse inventory system." {
            tags "Software System" "Logical"
            warehouseWebServer = container "Warehouse WebServer" "The web server." {
                tags "Container" "Web Server" "Logical"
                warehouseProductSyncController = component "ProductSyncController" "Handles product sync from PM." {
                    tags "Component"
                }
                warehouseProductController = component "ProductController" "Displays product list." {
                    tags "Component"
                }
                warehouseDeliveryController = component "DeliveryController" "Handles delivery management." {
                    tags "Component"
                }
                warehouseStockService = component "StockService" "Handles stock sync to Webshop." {
                    tags "Component" "Service"
                }
                warehouseProductRepository = component "ProductRepository" "Handles data access for products." {
                    tags "Component" "Repository"
                }
                warehouseDeliveryRepository = component "DeliveryRepository" "Handles data access for deliveries." {
                    tags "Component" "Repository"
                }
            }
            warehouseDatabase = container "Warehouse Database" "The database." {
                tags "Container" "Database" "Logical"
            }
        }

        // --- Logical Relationships (Business View) ---
        customer -> webshop "Uses" {
            tags "Logical"
        }
        productManager -> productManagementSystem "Uses" {
            tags "Logical"
        }
        warehouseStaff -> warehouseService "Uses" {
            tags "Logical"
        }
        productManagementSystem -> webshop "Sends product updates to" {
            tags "Logical"
        }
        productManagementSystem -> warehouseService "Sends product updates to" {
            tags "Logical"
        }
        warehouseService -> webshop "Sends stock updates to" {
            tags "Logical"
        }

        // --- Infrastructure Relationships (Physical/Routing View) ---
        // Point to the Container (Nginx) instead of the System (Gateway)
        customer -> reverseProxy "Uses (HTTPS)" {
            tags "Infrastructure"
        }
        productManager -> reverseProxy "Uses (HTTPS)" {
            tags "Infrastructure"
        }
        warehouseStaff -> reverseProxy "Uses (HTTPS)" {
            tags "Infrastructure"
        }

        // Gateway Routing
        reverseProxy -> webServer "Routes to (HTTP)" {
            tags "Infrastructure"
        }
        reverseProxy -> pmWebServer "Routes to (HTTP)" {
            tags "Infrastructure"
        }
        reverseProxy -> warehouseWebServer "Routes to (HTTP)" {
            tags "Infrastructure"
        }

        // Internal System-to-System via Gateway (Infrastructure View)
        pmWebServer -> reverseProxy "Sends product updates to (HTTPS)" {
            tags "Infrastructure" "Implementation"
        }
        warehouseWebServer -> reverseProxy "Sends stock updates to (HTTPS)" {
            tags "Infrastructure" "Implementation"
        }

        // Database Access (Direct - Logical & Infra)
        webServer -> database "Reads from and writes to" {
            tags "Implementation" "Logical"
        }
        pmWebServer -> pmDatabase "Reads from and writes to" {
            tags "Implementation" "Logical"
        }
        warehouseWebServer -> warehouseDatabase "Reads from and writes to" {
            tags "Implementation" "Logical"
        }

        // Direct Container Links (Logical/Current Implementation without Proxy)
        // These should be excluded from Infrastructure View if we want to show Proxy routing
        pmWebServer -> webServer "Sends product updates to (HTTP)" {
            tags "Implementation" "Direct"
        }
        pmWebServer -> warehouseWebServer "Sends product updates to (HTTP)" {
            tags "Implementation" "Direct"
        }
        warehouseWebServer -> webServer "Sends stock updates to (HTTP)" {
            tags "Implementation" "Direct"
        }


        // Component-level relationships for webshop
        productController -> productRepository "Uses"
        productSyncController -> productRepository "Uses"
        stockSyncController -> productRepository "Uses"
        productRepository -> database "Reads from and writes to"

        // Component-level relationships for productManagementSystem
        pmProductController -> pmProductService "Uses"
        pmProductService -> pmProductRepository "Uses"
        pmProductService -> productSyncController "Sends updates to"
        pmProductService -> warehouseProductSyncController "Sends updates to"
        pmProductRepository -> pmDatabase "Reads from and writes to"

        // Component-level relationships for warehouseService
        warehouseProductController -> warehouseProductRepository "Uses"
        warehouseProductSyncController -> warehouseProductRepository "Uses"
        warehouseDeliveryController -> warehouseDeliveryRepository "Uses"
        warehouseDeliveryController -> warehouseStockService "Uses"
        warehouseStockService -> stockSyncController "Sends stock updates to"
        warehouseProductRepository -> warehouseDatabase "Reads from and writes to"
        warehouseDeliveryRepository -> warehouseDatabase "Reads from and writes to"
    }

    views {
        // Logical View: Shows business context, hides infrastructure
        systemLandscape "SystemLandscape" "System Landscape" {
            include *
            exclude "element.tag==Infrastructure"
            exclude "relationship.tag==Infrastructure"
            autolayout tb
        }

        // Infrastructure View: Shows Gateway and routing
        container gateway "Infrastructure_View" "Infrastructure" {
            include "element.tag==Infrastructure"
            include "relationship.tag==Infrastructure"
            include "element.tag==Web Server"
            include "element.tag==Person"
            // Exclude direct links to show only Proxy routing
            exclude "relationship.tag==Direct"
            exclude "relationship.tag==Logical"
            autolayout tb
        }

        container webshop "Webshop_Containers" "Webshop - Containers" {
            include *
            exclude "element.tag==Infrastructure"
            exclude "relationship.tag==Infrastructure"
            autolayout tb
        }

        component webServer "Webshop_Components" "Webshop - Components" {
            include *
            autolayout tb
        }

        container productManagementSystem "PM_Containers" "Product Management - Containers" {
            include *
            exclude "element.tag==Infrastructure"
            exclude "relationship.tag==Infrastructure"
            autolayout tb
        }

        component pmWebServer "PM_Components" "Product Management - Components" {
            include *
            autolayout tb
        }

        container warehouseService "Warehouse_Containers" "Warehouse - Containers" {
            include *
            exclude "element.tag==Infrastructure"
            exclude "relationship.tag==Infrastructure"
            autolayout tb
        }

        component warehouseWebServer "Warehouse_Components" "Warehouse - Components" {
            include *
            autolayout tb
        }

        styles {
            element "Person" {
                shape Person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Web Server" {
                shape WebBrowser
            }
            element "Database" {
                shape Cylinder
                background #999999
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Repository" {
                background #999999
            }
            element "Service" {
                background #666666
                color #ffffff
            }
            element "Infrastructure" {
                background #333333
                color #ffffff
            }
        }
    }
}
