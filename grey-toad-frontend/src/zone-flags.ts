(window as any).__zone_symbol__UNPATCHED_EVENTS = ['unload', 'beforeunload'];

// Block unload/beforeunload registrations before Zone.js can add them (Chrome deprecation fix)
const _native = EventTarget.prototype.addEventListener;
EventTarget.prototype.addEventListener = function(type: string, listener: any, options?: any) {
  if (type === 'unload' || type === 'beforeunload') return;
  return _native.call(this, type, listener, options);
};
