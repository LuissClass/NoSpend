/**
 * NoSpend – Personal Expense Control
 *
 * Single-page React application that lets a user:
 *  - View the total balance and the spendable ("EXPENSES") balance of a bank account.
 *  - Browse the latest bank movements.
 *  - Manage budget categories and transfer money between them.
 *  - Import bank movements from a CSV file (preview first, then confirm).
 *
 * Business rules reflected in the UI:
 *  - "Total money" only changes when a CSV file is imported.
 *  - The "general expenses" category is the spendable pool.
 *  - Non-general categories only change through manual transfers.
 */

import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Upload,
  WalletCards,
  ArrowLeftRight,
  Tags,
  ReceiptText,
  Plus,
  RefreshCw,
  AlertTriangle,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  api,
  getAccounts,
  getAccount,
  getCategories,
  getMovements,
  getExpenses,
} from "./lib/api";
import { Card, Button, Input, Badge } from "./components/ui";
import "./index.css";

/**
 * Formats a numeric value as euros using the Spanish locale (e.g. 1.234,56 €).
 * Null, undefined or empty values are treated as 0.
 */
const money = (n) =>
  new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" }).format(
    Number(n || 0),
  );

/**
 * Root component. Holds all application state and the handlers that
 * communicate with the backend API; child components are purely presentational.
 */
function App() {
  // ---- Application state ----
  const [accounts, setAccounts] = useState([]), // All accounts available to the user
    [account, setAccount] = useState(null), // Currently selected account
    [categories, setCategories] = useState([]), // Categories of the selected account
    [movements, setMovements] = useState([]), // Latest bank movements
    [expenses, setExpenses] = useState({}), // Expense totals keyed by name (used by the dashboard)
    [tab, setTab] = useState("dashboard"), // Active tab: dashboard | movements | categories | import
    [file, setFile] = useState(null), // CSV file selected for import
    [preview, setPreview] = useState(null), // Import analysis result awaiting confirmation
    [busy, setBusy] = useState(false), // Global loading flag to disable actions
    [error, setError] = useState(""); // Last error message shown to the user
  const [filter, setFilter] = useState("all"); // Category filter for movements ("all" or a category id)

  /**
   * Loads every piece of data for the given account in parallel
   * and refreshes the local state.
   */
  const load = async (id) => {
    setBusy(true);
    try {
      const [a, c, m, e] = await Promise.all([
        getAccount(id),
        getCategories(id),
        getMovements(id, 50), // Latest 50 movements
        getExpenses(id),
      ]);
      setAccount(a);
      setCategories(c);
      setMovements(m);
      setExpenses(e);
    } catch (e) {
      // Prefer the backend error message when available
      setError(e.response?.data?.error || e.message);
    } finally {
      setBusy(false);
    }
  };

  // On first render: fetch accounts and automatically load the first one
  useEffect(() => {
    getAccounts()
      .then((x) => {
        setAccounts(x);
        if (x[0]) load(x[0].id);
      })
      .catch((e) => setError(e.message));
  }, []);

  // ---- Derived values ----
  // General (spendable) category, and the sum of manually allocated money
  // across all the other categories.
  const expensesCat = categories.find((c) => c.generalExpenses),
    allocated = categories
      .filter((c) => !c.generalExpenses)
      .reduce((s, c) => s + Number(c.availableBalance), 0);

  // Movements filtered by the selected category
  const visible =
    filter === "all"
      ? movements
      : movements.filter((m) => String(m.categoryId) === filter);

  /**
   * Step 1 of the import workflow: uploads the CSV so the backend can analyze it.
   * The response is a preview (accepted / ignored / errors) that the user must confirm.
   */
  async function importFile() {
    if (!file || !account) return;
    setBusy(true);
    setError("");
    try {
      const fd = new FormData();
      fd.append("file", file);
      const r = await api.post(`/accounts/${account.id}/imports`, fd);
      setPreview(r.data);
    } catch (e) {
      setError(e.response?.data?.error || e.message);
    } finally {
      setBusy(false);
    }
  }

  /**
   * Step 2 of the import workflow: confirms the previewed import,
   * clears the import form and reloads the account data.
   */
  async function confirm() {
    setBusy(true);
    try {
      await api.post(`/accounts/${account.id}/imports/confirm`, preview);
      setPreview(null);
      setFile(null);
      await load(account.id);
    } catch (e) {
      setError(e.response?.data?.error || e.message);
    } finally {
      setBusy(false);
    }
  }

  /** Creates a new category for the current account and refreshes data. */
  async function createCategory(name) {
    await api.post(`/accounts/${account.id}/categories`, { description: name });
    await load(account.id);
  }

  /** Assigns a movement to a category and refreshes data. */
  async function assign(movementId, categoryId) {
    await api.patch(
      `/accounts/${account.id}/movements/${movementId}/category`,
      { categoryId: Number(categoryId) },
    );
    await load(account.id);
  }

  /** Transfers an amount between two categories and refreshes data. */
  async function transfer(from, to, amount) {
    await api.post(`/accounts/${account.id}/categories/transfer`, {
      fromCategoryId: Number(from),
      toCategoryId: Number(to),
      amount: Number(amount),
    });
    await load(account.id);
  }

  return (
    <div className="min-h-screen">
      {/* Sticky header: brand and account selector */}
      <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur sticky top-0 z-10">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div>
            <div className="text-xl font-black tracking-tight">NoSpend</div>
            <div className="text-xs text-slate-500">
              Personal Expense Control
            </div>
          </div>
          {/* Switching account triggers a full data reload */}
          <select
            className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2"
            value={account?.id || ""}
            onChange={(e) => load(e.target.value)}
          >
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.holder}
              </option>
            ))}
          </select>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-6 py-8">
        {/* Tab navigation: [key, label, icon] */}
        <nav className="mb-8 flex flex-wrap gap-2">
          {[
            ["dashboard", "Overview", WalletCards],
            ["movements", "Movements", ReceiptText],
            ["categories", "Categories", Tags],
            ["import", "Import CSV", Upload],
          ].map(([k, t, I]) => (
            <Button
              key={k}
              variant={tab === k ? "primary" : "secondary"}
              onClick={() => setTab(k)}
            >
              <span className="flex items-center gap-2">
                <I size={16} />
                {t}
              </span>
            </Button>
          ))}
        </nav>

        {/* Global error banner */}
        {error && (
          <div className="mb-5 flex items-center gap-2 rounded-xl border border-red-900 bg-red-950/50 p-4 text-red-300">
            <AlertTriangle size={18} />
            {error}
          </div>
        )}

        {/* Active tab content */}
        {tab === "dashboard" && (
          <Dashboard
            account={account}
            categories={categories}
            expenses={expenses}
            allocated={allocated}
          />
        )}
        {tab === "movements" && (
          <Movements
            movements={visible}
            categories={categories}
            filter={filter}
            setFilter={setFilter}
            onAssign={assign}
          />
        )}
        {tab === "categories" && (
          <Categories
            categories={categories}
            onCreate={createCategory}
            onTransfer={transfer}
          />
        )}
        {tab === "import" && (
          <Import
            file={file}
            setFile={setFile}
            onImport={importFile}
            preview={preview}
            onConfirm={confirm}
            onCancel={() => setPreview(null)}
            busy={busy}
          />
        )}
      </main>
    </div>
  );
}

/**
 * Overview tab: summary cards (total, available, allocated)
 * and the list of balances per category.
 */
function Dashboard({ account, categories, expenses, allocated }) {
  // Converts the expenses object into an array of { name, value } (chart-ready format)
  const data = Object.entries(expenses).map(([name, value]) => ({
    name,
    value: Number(value),
  }));
  return (
    <div className="space-y-6">
      <div className="grid gap-5 md:grid-cols-3">
        {/* Total money in the account (only updated by CSV imports) */}
        <Card className="p-6">
          <p className="text-sm text-slate-400">TOTAL MONEY</p>
          <p className="mt-2 text-4xl font-black">
            {money(account?.totalBalance)}
          </p>
          <p className="mt-2 text-xs text-slate-500">
            Only changes on CSV import
          </p>
        </Card>
        {/* Spendable pool: shown in red when negative, green otherwise */}
        <Card className="p-6">
          <p className="text-sm text-slate-400">AVAILABLE BALANCE</p>
          <p
            className={`mt-2 text-4xl font-black ${(categories.find((c) => c.generalExpenses)?.availableBalance || 0) < 0 ? "text-red-400" : "text-emerald-400"}`}
          >
            {money(categories.find((c) => c.generalExpenses)?.availableBalance)}
          </p>
          <p className="mt-2 text-xs text-slate-500">
            EXPENSES / spendable pool
          </p>
        </Card>
        {/* Sum of money manually allocated to non-general categories */}
        <Card className="p-6">
          <p className="text-sm text-slate-400">ALLOCATED</p>
          <p className="mt-2 text-4xl font-black">{money(allocated)}</p>
          <p className="mt-2 text-xs text-slate-500">
            Manual category allocations
          </p>
        </Card>
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Balance per category */}
        <Card className="p-6">
          <h2 className="mb-5 text-lg font-bold">Money by category</h2>
          <div className="space-y-3">
            {categories.map((c) => (
              <div
                key={c.id}
                className="flex items-center justify-between rounded-xl bg-slate-950 p-4"
              >
                <span>{c.description}</span>
                <span className="font-bold">{money(c.availableBalance)}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

/**
 * Movements tab: table with the latest bank movements.
 * Negative amounts are shown in red, positive amounts in green.
 *
 * Note: `categories`, `filter`, `setFilter` and `onAssign` are received
 * but not yet used in the UI (category filter and assignment controls pending).
 */
function Movements({ movements, categories, filter, setFilter, onAssign }) {
  return (
    <Card className="overflow-hidden">
      <div className="flex items-center justify-between border-b border-slate-800 p-5">
        <div>
          <h2 className="text-lg font-bold">Movements</h2>
          <p className="text-sm text-slate-500">Latest 50 by default</p>
        </div>
      </div>
      {/* Horizontal scroll for small screens */}
      <div className="overflow-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-950 text-slate-500">
            <tr>
              <th className="p-4">Date</th>
              <th>Concept</th>
              <th className="text-right p-4">Amount</th>
              <th className="text-right p-4">Available balance</th>
            </tr>
          </thead>
          <tbody>
            {movements.map((m) => (
              <tr key={m.id} className="border-t border-slate-800">
                <td className="p-4">{m.date}</td>
                <td>{m.concept}</td>
                <td
                  className={`p-4 text-right font-semibold ${Number(m.amount) < 0 ? "text-red-300" : "text-emerald-300"}`}
                >
                  {money(m.amount)}
                </td>
                <td className="p-4 text-right">{money(m.availableBalance)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

/**
 * Categories tab: create new categories and transfer money between them.
 * Form state is local; the actual API calls are delegated to the parent
 * through `onCreate` and `onTransfer`.
 */
function Categories({ categories, onCreate, onTransfer }) {
  const [name, setName] = useState(""), // New category name
    [from, setFrom] = useState(""), // Source category id for a transfer
    [to, setTo] = useState(""), // Destination category id for a transfer
    [amount, setAmount] = useState(""); // Amount to transfer
  const nonGeneral = categories.filter((c) => !c.generalExpenses);
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      {/* Create category + list of existing categories */}
      <Card className="p-6">
        <h2 className="mb-4 text-lg font-bold">Category management</h2>
        <div className="flex gap-2">
          <Input
            placeholder="e.g. Saving"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <Button
            onClick={() => {
              onCreate(name);
              setName(""); // Reset the input after submitting
            }}
          >
            <Plus size={18} />
          </Button>
        </div>
        <div className="mt-6 space-y-3">
          {categories.map((c) => (
            <div
              key={c.id}
              className="flex items-center justify-between rounded-xl bg-slate-950 p-4"
            >
              <div>
                <div className="font-semibold">{c.description}</div>
                {c.generalExpenses && <Badge>General</Badge>}
              </div>
              <div className="font-bold">{money(c.availableBalance)}</div>
            </div>
          ))}
        </div>
      </Card>

      {/* Transfer form */}
      <Card className="p-6">
        <h2 className="mb-4 text-lg font-bold">Transfer money</h2>
        <div className="space-y-3">
          <select
            className="w-full rounded-xl border border-slate-700 bg-slate-950 p-2"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          >
            <option value="">From</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.description}
              </option>
            ))}
          </select>
          <select
            className="w-full rounded-xl border border-slate-700 bg-slate-950 p-2"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          >
            <option value="">To</option>
            {/* Prevent selecting the same category as source and destination */}
            {categories
              .filter((c) => c.id !== Number(from))
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.description}
                </option>
              ))}
          </select>
          <Input
            type="number"
            step="0.01"
            placeholder="Amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Button onClick={() => onTransfer(from, to, amount)}>
            <ArrowLeftRight size={18} /> Transfer
          </Button>
          <p className="text-xs text-slate-500">
            Non-general categories only change through manual transfers.
          </p>
        </div>
      </Card>
    </div>
  );
}

/**
 * Import tab: two-step CSV import.
 *  1. The user selects a file and clicks "Analyze CSV" (backend returns a preview).
 *  2. The user reviews the summary (accepted / ignored / errors) and confirms or cancels.
 */
function Import({
  file,
  setFile,
  onImport,
  preview,
  onConfirm,
  onCancel,
  busy,
}) {
  return (
    <Card className="p-6">
      <h2 className="text-xl font-bold">Import bank CSV</h2>
      <p className="mt-1 text-sm text-slate-500">
        Workflow: CSV → Importing → Examining → Added / Ignored.
      </p>
      {/* File picker: the whole area acts as a clickable label for the hidden input */}
      <label className="mt-6 flex cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed border-slate-700 bg-slate-950 p-10">
        <Upload size={28} />
        <span className="mt-2">{file?.name || "Choose CSV file"}</span>
        <input
          className="hidden"
          type="file"
          accept=".csv,text/csv"
          onChange={(e) => setFile(e.target.files?.[0])}
        />
      </label>
      <div className="mt-4 flex gap-2">
        {file && (
          <Button onClick={onImport} disabled={busy}>
            {busy ? "Analyzing..." : "Analyze CSV"}
          </Button>
        )}
      </div>

      {/* Import preview, shown only after the file has been analyzed */}
      {preview && (
        <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-950 p-5">
          <h3 className="font-bold">Import summary</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            <Badge>Accepted: {preview.accepted.length}</Badge>
            <Badge>Ignored: {preview.ignored.length}</Badge>
            <Badge>Errors: {preview.errors.length}</Badge>
          </div>
          {/* Detailed list of errors found during analysis */}
          {preview.errors.length > 0 && (
            <div className="mt-4 space-y-2 text-sm text-amber-300">
              {preview.errors.map((e, i) => (
                <div key={i}>• {e}</div>
              ))}
            </div>
          )}
          <div className="mt-5 flex gap-2">
            <Button onClick={onConfirm} disabled={busy}>
              Confirm import
            </Button>
            <Button variant="secondary" onClick={onCancel}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
}

// Application entry point
createRoot(document.getElementById("root")).render(<App />);