import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(sales-access)/passes-tickets')({ component: PassesTickets })

function PassesTickets() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Passes &amp; Tickets</h1>
      <p className="mt-4 text-lg">
        Manage passes and tickets.
      </p>
    </div>
  )
}
