workspace "My System" "My System Description" {
    model {
        customer = person "Customer" "A customer of the webshop."
        productManager = person "Product Manager" "A manager of products"

        webshop = softwareSystem "Webshop" "The webshop system." {
            webServer = container "Webshop WebServer" "The web server." {
                productController = component "ProductController" "Handles product listing."
                productRepository = component "ProductRepository" "Handles data access for products."
            }
            database = container "Webshop Database" "The database."
        }

        productManagementSystem = softwareSystem "Product Management System" "The Product Management System" {
            pmWebServer = container "PM WebServer" "The web server." {
                pmProductController = component "ProductController" "Handles product management (CRUD)."
                pmProductRepository = component "ProductRepository" "Handles data access for products."
            }
            pmDatabase = container "PM Database" "The database."
        }

        // High-level relationship
        customer -> webshop "Uses"
        productManager -> productManagementSystem "Uses"

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

        // Component-level relationships for webshop
        productController -> productRepository "Uses"
        productRepository -> database "Reads from and writes to"

        // Component-level relationships for productManagementSystem
        pmProductController -> pmProductRepository "Uses"
        pmProductRepository -> pmDatabase "Reads from and writes to"
    }

    views {
        systemLandscape "SystemLandscape" {
            include *
            autolayout
        }

        container webshop "Containers" {
            include *
            autolayout
        }

        component webServer "Components" {
            include *
            autolayout
        }

        container productManagementSystem "PM_Containers" {
            include *
            autolayout
        }

        component pmWebServer "PM_Components" {
            include *
            autolayout
        }
    }
}