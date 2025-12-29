# KNOWN BUGS

## FIXED: ProductManagerServer shall not syncronize to WebShop before the user pressed the sync-button.
(Fixed) Now it requires manual sync.

## FIXED: Ordermanager doesn't confim the order. 
(Fixed) The order is now created as PENDING_CONFIRMATION and requires manual confirmation.

## FIXED: Customers order doesn't update
(Fixed) Status updates (Paid, Shipped) are now propagated to the Order Service.