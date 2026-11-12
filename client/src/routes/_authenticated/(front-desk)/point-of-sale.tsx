import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(front-desk)/point-of-sale')({ component: PointOfSale })

function PointOfSale() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Point of Sale</h1>
      <p className="mt-4 text-lg">
        Manage point of sale purchases.
      </p>
    </div>
  )
}
