'use client'

import React, { useState } from 'react'

// ─── Types ────────────────────────────────────────────────────────────────────

type Accessor<T> = keyof T | ((item: T) => React.ReactNode)

interface Column<T> {
  header: string
  accessor: Accessor<T>
}

interface DataTableProps<T> {
  data: T[]
  columns: Column<T>[]
  loading?: boolean
  onPageChange?: (page: number) => void
  pageSize?: number
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function resolveCell<T>(item: T, accessor: Accessor<T>): React.ReactNode {
  if (typeof accessor === 'function') {
    return accessor(item)
  }
  const value = item[accessor]
  if (value === null || value === undefined) return '—'
  return String(value)
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function DataTable<T extends object>({
  data,
  columns,
  loading = false,
  onPageChange,
  pageSize = 10,
}: DataTableProps<T>) {
  const [currentPage, setCurrentPage] = useState(1)

  const totalPages = Math.max(1, Math.ceil(data.length / pageSize))
  const paginatedData = data.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  )

  function handlePrev() {
    if (currentPage <= 1) return
    const next = currentPage - 1
    setCurrentPage(next)
    onPageChange?.(next)
  }

  function handleNext() {
    if (currentPage >= totalPages) return
    const next = currentPage + 1
    setCurrentPage(next)
    onPageChange?.(next)
  }

  // ── Loading skeleton ───────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="w-full rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              {columns.map((col) => (
                <th
                  key={col.header}
                  className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide"
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: pageSize }).map((_, i) => (
              <tr key={i} className="border-b border-gray-100 last:border-none">
                {columns.map((col) => (
                  <td key={col.header} className="px-4 py-3">
                    <div className="h-4 bg-gray-200 rounded animate-pulse w-3/4" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }

  // ── Empty state ────────────────────────────────────────────────────────────
  if (data.length === 0) {
    return (
      <div className="w-full rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              {columns.map((col) => (
                <th
                  key={col.header}
                  className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide"
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
        </table>
        <div className="flex flex-col items-center justify-center py-16 text-gray-400 gap-2">
          <svg
            className="w-10 h-10 text-gray-300"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M3 10h18M3 14h18M10 3v18M14 3v18"
            />
          </svg>
          <p className="text-sm font-medium">No hay datos disponibles</p>
        </div>
      </div>
    )
  }

  // ── Table ──────────────────────────────────────────────────────────────────
  return (
    <div className="w-full flex flex-col gap-3">
      <div className="rounded-xl border border-gray-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-200">
              {columns.map((col) => (
                <th
                  key={col.header}
                  className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide whitespace-nowrap"
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {paginatedData.map((item, rowIdx) => (
              <tr
                key={rowIdx}
                className="hover:bg-gray-50 transition-colors duration-100"
              >
                {columns.map((col) => (
                  <td
                    key={col.header}
                    className="px-4 py-3 text-gray-700 align-middle"
                  >
                    {resolveCell(item, col.accessor)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between px-1 text-sm text-gray-500">
        <span>
          Página{' '}
          <span className="font-semibold text-gray-700">{currentPage}</span> de{' '}
          <span className="font-semibold text-gray-700">{totalPages}</span>
          {' · '}
          <span className="text-gray-400">{data.length} registros</span>
        </span>

        <div className="flex gap-2">
          <button
            onClick={handlePrev}
            disabled={currentPage === 1}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gray-200
                       text-gray-600 text-xs font-medium
                       hover:bg-gray-100 hover:border-gray-300
                       disabled:opacity-40 disabled:cursor-not-allowed
                       transition-colors duration-150"
          >
            ← Anterior
          </button>
          <button
            onClick={handleNext}
            disabled={currentPage === totalPages}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gray-200
                       text-gray-600 text-xs font-medium
                       hover:bg-gray-100 hover:border-gray-300
                       disabled:opacity-40 disabled:cursor-not-allowed
                       transition-colors duration-150"
          >
            Siguiente →
          </button>
        </div>
      </div>
    </div>
  )
}
