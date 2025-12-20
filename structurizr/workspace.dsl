workspace "My System" "My System Description" {
    model {
        customer = person "Customer" "A customer of the webshop." {
            tags "Person"
        }
        productManager = person "Product Manager" "A manager of products" {
            tags "Person"
        }

        webshop = softwareSystem "Webshop" "The webshop system." {
            tags "Software System"
            webServer = container "Webshop WebServer" "The web server." {
                tags "Container" "Web Server"
                productController = component "ProductController" "Handles product listing." {
                    tags "Component"
                }
                productSyncController = component "ProductSyncController" "Handles product synchronization API." {
                    tags "Component"
                }
                productRepository = component "ProductRepository" "Handles data access for products." {
                    tags "Component" "Repository"
                }
            }
            database = container "Webshop Database" "The database." {
                tags "Container" "Database"
            }
        }

        productManagementSystem = softwareSystem "Product Management System" "The Product Management System" {
            tags "Software System"
            pmWebServer = container "PM WebServer" "The web server." {
                tags "Container" "Web Server"
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
                tags "Container" "Database"
            }
        }

        // High-level relationship
        customer -> webshop "Uses"
        productManager -> productManagementSystem "Uses"
        productManagementSystem -> webshop "Sends product updates to"

        // Container-level relationships for webshop
        customer -> webServer "Uses" {
            tags "Implementation"
        }
        webServer -> database "Reads from and writes to" {
            tags "Implementation"
        }

        // Container-level relationships for productManagementSystem
        productManager -> pmWebServer "Uses" {
            tags "Implementation"
        }
        pmWebServer -> pmDatabase "Reads from and writes to" {
            tags "Implementation"
        }
        // PM System sends updates to Webshop
        pmWebServer -> webServer "Sends product updates to (HTTP)" {
            tags "Implementation"
        }

        // Component-level relationships for webshop
        productController -> productRepository "Uses"
        productSyncController -> productRepository "Uses"
        productRepository -> database "Reads from and writes to"

        // Component-level relationships for productManagementSystem
        pmProductController -> pmProductService "Uses"
        pmProductService -> pmProductRepository "Uses"
        pmProductService -> productSyncController "Sends updates to"
        pmProductRepository -> pmDatabase "Reads from and writes to"
    }

    views {
        systemLandscape "SystemLandscape" {
            include *
            autolayout tb
        }

        container webshop "Webshop_Containers" {
            include *
            autolayout tb
        }

        component webServer "Webshop_Components" {
            include *
            autolayout tb
        }

        container productManagementSystem "PM_Containers" {
            include *
            autolayout tb
        }

        component pmWebServer "PM_Components" {
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
        }
    }
}
