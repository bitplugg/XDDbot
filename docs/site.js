(() => {
  const panels = Array.from(document.querySelectorAll(".panel"));
  const tabs = Array.from(document.querySelectorAll(".tab"));

  function show(id) {
    const target = panels.find((p) => p.id === id) || panels[0];
    if (!target) return;
    panels.forEach((p) => p.classList.toggle("active", p === target));
    tabs.forEach((t) => t.classList.toggle("active", t.dataset.panel === target.id));
    document.title = `${target.dataset.title || "XDDBOT"} · XDDBOT`;
  }

  function fromHash() {
    const id = location.hash.replace(/^#/, "");
    if (id && panels.some((p) => p.id === id)) {
      show(id);
      return true;
    }
    return false;
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", (e) => {
      e.preventDefault();
      const id = tab.dataset.panel;
      history.replaceState(null, "", `#${id}`);
      show(id);
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  });

  window.addEventListener("hashchange", fromHash);

  if (!fromHash()) show(panels[0]?.id);

  // copy buttons
  document.querySelectorAll("pre").forEach((pre) => {
    const wrap = document.createElement("div");
    wrap.className = "code-wrap";
    pre.parentNode.insertBefore(wrap, pre);
    wrap.appendChild(pre);
    const btn = document.createElement("button");
    btn.className = "copy";
    btn.type = "button";
    btn.textContent = "copy";
    btn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(pre.innerText);
        btn.textContent = "ok";
      } catch {
        btn.textContent = "err";
      }
      setTimeout(() => (btn.textContent = "copy"), 1200);
    });
    wrap.appendChild(btn);
  });
})();
