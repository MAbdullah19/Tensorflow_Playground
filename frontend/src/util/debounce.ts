/**
 * Minimal debounce that schedules the call on the trailing edge.
 * Used to avoid spamming the boundary endpoint when sliders are dragged.
 */
export function debounce<TArgs extends unknown[]>(
  fn: (...args: TArgs) => void,
  ms: number,
): ((...args: TArgs) => void) & { cancel: () => void } {
  let timer: number | undefined;
  const wrapped = (...args: TArgs) => {
    if (timer !== undefined) window.clearTimeout(timer);
    timer = window.setTimeout(() => fn(...args), ms);
  };
  wrapped.cancel = () => {
    if (timer !== undefined) {
      window.clearTimeout(timer);
      timer = undefined;
    }
  };
  return wrapped;
}
