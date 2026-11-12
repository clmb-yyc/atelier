import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/transactions')({ component: Transactions })

function Transactions() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Transactions</h1>
      <p className="mt-4 text-lg">
        View and manage transactions.
      </p>
    </div>
  )
}
