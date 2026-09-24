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

const money = (n) =>
  new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" }).format(
    Number(n || 0),
  );
function App() {
  const [accounts, setAccounts] = useState([]),
    [account, setAccount] = useState(null),
    [categories, setCategories] = useState([]),
    [movements, setMovements] = useState([]),
    [expenses, setExpenses] = useState({}),
    [tab, setTab] = useState("dashboard"),
    [file, setFile] = useState(null),
    [preview, setPreview] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [filter, setFilter] = useState("all");
  const load = async (id) => {
    setBusy(true);
    try {
      const [a, c, m, e] = await Promise.all([
        getAccount(id),
        getCategories(id),
        getMovements(id, 50),
        getExpenses(id),
      ]);
      setAccount(a);
      setCategories(c);
      setMovements(m);
      setExpenses(e);
    } catch (e) {
      setError(e.response?.data?.error || e.message);
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    getAccounts()
      .then((x) => {
        setAccounts(x);
        if (x[0]) load(x[0].id);
      })
      .catch((e) => setError(e.message));
  }, []);
  const expensesCat = categories.find((c) => c.generalExpenses),
    allocated = categories
      .filter((c) => !c.generalExpenses)
      .reduce((s, c) => s + Number(c.availableBalance), 0);
  const visible =
    filter === "all"
      ? movements
      : movements.filter((m) => String(m.categoryId) === filter);
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
  async function createCategory(name) {
    await api.post(`/accounts/${account.id}/categories`, { description: name });
    await load(account.id);
  }
  async function assign(movementId, categoryId) {
    await api.patch(
      `/accounts/${account.id}/movements/${movementId}/category`,
      { categoryId: Number(categoryId) },
    );
    await load(account.id);
  }
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
      <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur sticky top-0 z-10">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div>
            <div className="text-xl font-black tracking-tight">NoSpend</div>
            <div className="text-xs text-slate-500">
              Personal Expense Control
            </div>
          </div>
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
        {error && (
          <div className="mb-5 flex items-center gap-2 rounded-xl border border-red-900 bg-red-950/50 p-4 text-red-300">
            <AlertTriangle size={18} />
            {error}
          </div>
        )}
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

function Dashboard({ account, categories, expenses, allocated }) {
  const data = Object.entries(expenses).map(([name, value]) => ({
    name,
    value: Number(value),
  }));
  return (
    <div className="space-y-6">
      <div className="grid gap-5 md:grid-cols-3">
        <Card className="p-6">
          <p className="text-sm text-slate-400">TOTAL MONEY</p>
          <p className="mt-2 text-4xl font-black">
            {money(account?.totalBalance)}
          </p>
          <p className="mt-2 text-xs text-slate-500">
            Only changes on CSV import
          </p>
        </Card>
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
        <Card className="p-6">
          <p className="text-sm text-slate-400">ALLOCATED</p>
          <p className="mt-2 text-4xl font-black">{money(allocated)}</p>
          <p className="mt-2 text-xs text-slate-500">
            Manual category allocations
          </p>
        </Card>
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
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

function Movements({ movements, categories, filter, setFilter, onAssign }) {
  return (
    <Card className="overflow-hidden">
      <div className="flex items-center justify-between border-b border-slate-800 p-5">
        <div>
          <h2 className="text-lg font-bold">Movements</h2>
          <p className="text-sm text-slate-500">Latest 50 by default</p>
        </div>
      </div>
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

function Categories({ categories, onCreate, onTransfer }) {
  const [name, setName] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [amount, setAmount] = useState("");
  const nonGeneral = categories.filter((c) => !c.generalExpenses);
  return (
    <div className="grid gap-6 lg:grid-cols-2">
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
              setName("");
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
      {preview && (
        <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-950 p-5">
          <h3 className="font-bold">Import summary</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-3">
            <Badge>Accepted: {preview.accepted.length}</Badge>
            <Badge>Ignored: {preview.ignored.length}</Badge>
            <Badge>Errors: {preview.errors.length}</Badge>
          </div>
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
createRoot(document.getElementById("root")).render(<App />);
