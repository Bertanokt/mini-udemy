import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * Sunucuda ve hydration sırasında `false`, tarayıcıda bileşen bağlandıktan sonra `true` (#256).
 *
 * Tarayıcıya özgü duruma (localStorage'daki oturum) bağlı çizimi hydration'dan sonraya bırakmak için. Hydration
 * sırasında React `getServerSnapshot`'ı kullandığı için, geç hydrate olan bir Suspense sınırında bile ilk çizim
 * sunucunun HTML'iyle aynı kalır. `useEffect` içinde `setState` yerine bu yol: ek bir çizim turu yok ve lint'e takılmaz.
 */
export function useHasMounted(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
