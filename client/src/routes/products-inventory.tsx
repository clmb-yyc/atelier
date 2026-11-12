import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/products-inventory')({ component: ProductsInventory })

function ProductsInventory() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Products &amp; Inventory</h1>
      <p className="mt-4 text-lg">
        Manage products and inventory.
      </p>
    </div>
  )
}
