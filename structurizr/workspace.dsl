workspace "My System" "My System Description" {
    model {
        customer = person "Customer" "A customer of the webshop."

        webshop = softwareSystem "Webshop" "The webshop system." {
            webServer = container "WebServer" "The web server." {
                productController = component "ProductController" "Handles product listing."
                productRepository = component "ProductRepository" "Handles data access for products."
            }
            database = container "Database" "The database."
        }

        // High-level relationship
        customer -> webshop "Uses"

        // Container-level relationships
        customer -> webServer "Uses" {
            tags "Implementation"
        }
        webServer -> database "Reads from and writes to" {
            tags "Implementation"
        }

        // Component-level relationships
        productController -> productRepository "Uses"
        productRepository -> database "Reads from and writes to"
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
    }
}