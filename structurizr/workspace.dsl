workspace "My System" "My System Description" {
    model {
        customer = person "Customer" "A customer of the webshop."

        webshop = softwareSystem "Webshop" "The webshop system." {
            webServer = container "WebServer" "The web server." {
                productController = component "ProductController" "Handles product listing."
            }
            database = container "Database" "The database."
        }

        // High-level relationship for context-level views and requirements
        customer -> webshop "Uses"

        // Detailed relationships for container-level views and requirements
        customer -> webServer "Uses" {
            tags "Implementation"
        }
        webServer -> database "Reads from and writes to" {
            tags "Implementation"
        }

        // Component-level relationship
        productController -> database "Reads products from"
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
