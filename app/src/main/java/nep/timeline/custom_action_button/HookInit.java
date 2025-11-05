package nep.timeline.custom_action_button;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage {
    private final int ACTION_BUTTON_CLICK = 781;
    private final int ACTION_BUTTON_LONG_PRESS = 782;

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static ClassLoader classLoader;

    private void openCamera(Object instance) {
        XposedHelpers.callMethod(instance, "n", XposedHelpers.callMethod(XposedHelpers.getStaticObjectField(XposedHelpers.findClassIfExists("m6.b", classLoader), "h"), "f", "camera"));
    }

    private void screenshot(Object instance) {
        XposedHelpers.callMethod(instance, "p");
    }

    private void switchRingMode(Object instance) {
        XposedHelpers.callMethod(instance, "v", "ring_mode");
    }

    private void onLongPress(Object instance) {

    }

    private void onDoubleClick(Object instance) {

    }

    private void onClick(Object instance) {

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageParam) {
        if ("com.oplus.gesture".equals(packageParam.packageName)) {
            classLoader = packageParam.classLoader;

            try {
                XposedHelpers.findAndHookConstructor("k8.l", classLoader, "android.content.Context", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        context = (Context) param.args[0];
                    }
                });

                XposedHelpers.findAndHookMethod("k8.l$a", classLoader, "onKeyEvent", "android.view.KeyEvent", new XC_MethodHook() {
                    private long lastClickTime = 0;
                    private final long DOUBLE_CLICK_THRESHOLD = 300;
                    private final Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        KeyEvent keyEvent = (KeyEvent) param.args[0];
                        int action = keyEvent.getAction();
                        int keyCode = keyEvent.getKeyCode();

                        try {
                            Object instance = XposedHelpers.callStaticMethod(XposedHelpers.findClassIfExists("m6.c0", classLoader), "k", context);
                            if (instance == null)
                                return;

                            if (action == KeyEvent.ACTION_DOWN && (keyCode == ACTION_BUTTON_CLICK || keyCode == ACTION_BUTTON_LONG_PRESS)) {
                                if (keyCode == ACTION_BUTTON_CLICK) {
                                    long currentTime = System.currentTimeMillis();
                                    long timeDifference = currentTime - lastClickTime;

                                    if (timeDifference <= DOUBLE_CLICK_THRESHOLD) {
                                        handler.removeCallbacksAndMessages(null);
                                        onDoubleClick(instance);
                                    } else
                                        handler.postDelayed(() -> {
                                            if (System.currentTimeMillis() - lastClickTime > DOUBLE_CLICK_THRESHOLD)
                                                onClick(instance);
                                        }, 500);

                                    lastClickTime = currentTime;
                                } else if (keyCode == ACTION_BUTTON_LONG_PRESS)
                                    onLongPress(instance);

                                param.setResult(null);
                            }
                        } catch (Throwable throwable) {
                            Log.e(GlobalVars.TAG, Log.getStackTraceString(throwable));
                        }
                    }
                });
            } catch (Throwable ignored) {
                XposedBridge.log(GlobalVars.TAG + " -> Your device is unsupported!");
            }
        }
    }
}
