package io.github.fentia.mmsclean;

import android.util.Log;

import org.json.JSONArray;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedModule;

public class MmsCleanHook extends XposedModule {

    private static final String TAG = "MmsClean";
    private static final String TARGET_PACKAGE = "com.android.mms";

    private static final String CLASS_MENU_INFO_BASE_BEAN =
            "com.opos.smart.mms.interfaces.netmsg.menu.MenuInfoBaseBean";
    private static final String CLASS_ACTION_BASE =
            "com.ted.android.data.bubbleAction.ActionBase";
    private static final String CLASS_BUBBLE_ENTITY = "com.ted.android.data.BubbleEntity";

    // 3/4 打开网页、6 打开应用、12 快递跳转、23 中国移动营销
    private static final Set<Integer> MARKETING_ACTIONS =
            new HashSet<>(Arrays.asList(3, 4, 6, 12, 23));

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;
        hookBottomInputBoxMenu(param.getClassLoader());
        hookCardMarketingButton(param.getClassLoader());
    }

    private void hookBottomInputBoxMenu(ClassLoader classLoader) {
        try {
            Method getMenus = Class.forName(CLASS_MENU_INFO_BASE_BEAN, false, classLoader)
                    .getDeclaredMethod("getMenus");
            hook(getMenus).intercept(chain -> new ArrayList<>());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "bottom input box menu hook failed", t);
        }
    }

    private void hookCardMarketingButton(ClassLoader classLoader) {
        try {
            Class<?> bubbleEntity = Class.forName(CLASS_BUBBLE_ENTITY, false, classLoader);
            Method fromJSONArray = Class.forName(CLASS_ACTION_BASE, false, classLoader)
                    .getDeclaredMethod("fromJSONArray", bubbleEntity, JSONArray.class);
            hook(fromJSONArray).intercept(chain -> removeMarketingActions(chain.proceed()));
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "card marketing button hook failed", t);
        }
    }

    // 从动作列表中剔除营销类动作
    private Object removeMarketingActions(Object result) {
        if (!(result instanceof List)) return result;
        List<?> actions = (List<?>) result;
        for (Iterator<?> iterator = actions.iterator(); iterator.hasNext(); ) {
            Object action = iterator.next();
            if (action == null || !MARKETING_ACTIONS.contains(actionType(action))) continue;
            iterator.remove();
        }
        return result;
    }

    // 读取 ActionBase.action 字段
    private static int actionType(Object action) {
        try {
            Field field = action.getClass().getField("action");
            return field.getInt(action);
        } catch (Throwable t) {
            return -1;
        }
    }
}
