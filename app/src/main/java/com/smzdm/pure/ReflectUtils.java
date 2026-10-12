package com.smzdm.pure;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

public final class ReflectUtils {
    private static final String TAG = "SMZDMPure";

    private ReflectUtils() {
    }

    public static Class<?> findClass(String className, ClassLoader cl) {
        if (className == null) return null;
        try {
            if (cl != null) {
                return Class.forName(className, false, cl);
            }
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null || methodName == null) return null;
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Method m = current.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                break;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static boolean hookMethod(XposedInterface xposed, Class<?> clazz, String methodName,
                                     Class<?>[] paramTypes, XposedInterface.Hooker hooker) {
        Method m = findMethod(clazz, methodName, paramTypes);
        if (m != null) {
            try {
                xposed.hook(m).intercept(hooker);
                return true;
            } catch (Throwable t) {
                Log.w(TAG, "hookMethod failed: " + clazz.getName() + "#" + methodName + " - " + t);
            }
        }
        return false;
    }

    public static boolean hookMethod(XposedInterface xposed, String className, ClassLoader cl,
                                     String methodName, Class<?>[] paramTypes, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClass(className, cl);
        if (clazz == null) return false;
        return hookMethod(xposed, clazz, methodName, paramTypes, hooker);
    }

    public static int hookAllMethods(XposedInterface xposed, Class<?> clazz, String methodName,
                                     XposedInterface.Hooker hooker) {
        if (clazz == null || methodName == null) return 0;
        int count = 0;
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    try {
                        m.setAccessible(true);
                        xposed.hook(m).intercept(hooker);
                        count++;
                    } catch (Throwable t) {
                        Log.w(TAG, "hookAllMethods failed for " + m + " - " + t);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "getDeclaredMethods failed on " + clazz.getName() + " - " + t);
        }
        return count;
    }

    public static int hookAllMethods(XposedInterface xposed, String className, ClassLoader cl,
                                     String methodName, XposedInterface.Hooker hooker) {
        Class<?> clazz = findClass(className, cl);
        if (clazz == null) return 0;
        return hookAllMethods(xposed, clazz, methodName, hooker);
    }

    public static int hookMethodNoOp(XposedInterface xposed, Class<?> clazz, String methodName, Object nonVoidReturn) {
        if (clazz == null || methodName == null) return 0;
        int count = 0;
        try {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    m.setAccessible(true);
                    Class<?> ret = m.getReturnType();
                    xposed.hook(m).intercept(chain -> {
                        if (ret == void.class) return null;
                        if (ret == boolean.class) return Boolean.TRUE.equals(nonVoidReturn);
                        if (ret == Boolean.class) return nonVoidReturn instanceof Boolean ? nonVoidReturn : Boolean.FALSE;
                        if (ret == int.class) return nonVoidReturn instanceof Integer ? nonVoidReturn : 0;
                        if (ret == Integer.class) return nonVoidReturn instanceof Integer ? nonVoidReturn : Integer.valueOf(0);
                        return nonVoidReturn;
                    });
                    count++;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "hookMethodNoOp failed on " + clazz.getName() + "#" + methodName + ": " + t);
        }
        return count;
    }

    public static int hookMethodNoOp(XposedInterface xposed, String className, ClassLoader cl,
                                     String methodName, Object nonVoidReturn) {
        Class<?> clazz = findClass(className, cl);
        if (clazz == null) return 0;
        return hookMethodNoOp(xposed, clazz, methodName, nonVoidReturn);
    }

    public static Object getField(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                break;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static void setField(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null) return;
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                break;
            }
            current = current.getSuperclass();
        }
    }

    public static void setBooleanField(Object target, String fieldName, boolean value) {
        if (target == null || fieldName == null) return;
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.setBoolean(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                break;
            }
            current = current.getSuperclass();
        }
    }

    public static Object invokeMethod(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        Method m = findMethod(target.getClass(), methodName);
        if (m != null) {
            try {
                return m.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static Object invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        if (target == null || methodName == null) return null;
        Method m = findMethod(target.getClass(), methodName, paramTypes);
        if (m != null) {
            try {
                return m.invoke(target, args);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
