import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(front-desk)/check-ins')({ component: CheckIns })

function CheckIns() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Check-ins</h1>
      <p className="mt-4 text-lg">
        Manage customer check-ins.
      </p>
    </div>
  )
}
