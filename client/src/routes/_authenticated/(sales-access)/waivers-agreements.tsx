import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(sales-access)/waivers-agreements')({ component: WaiversAgreements })

function WaiversAgreements() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Waivers &amp; Agreements</h1>
      <p className="mt-4 text-lg">
        Manage waivers and agreements.
      </p>
    </div>
  )
}
