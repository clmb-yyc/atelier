import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/customers-crm')({ component: CustomersCrm })

function CustomersCrm() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Customers / CRM</h1>
      <p className="mt-4 text-lg">
        Manage customers and their relationships.
      </p>
    </div>
  )
}
