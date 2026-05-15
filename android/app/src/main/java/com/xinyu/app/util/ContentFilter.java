package com.xinyu.app.util;

/**
 * 客户端内容审核 - 在发送前过滤敏感内容
 */
public class ContentFilter {

    private static final String[] BANNED_WORDS = {
        // 政治敏感
        "习近平", "毛泽东", "共产党", "六四", "天安门事件", "法轮功", "台独", "藏独", "疆独",
        "颠覆", "暴动", "政变",
        // 暴力
        "杀人", "自杀", "自残", "跳楼", "割腕", "上吊", "死", "死亡",
        "枪支", "炸弹", "爆炸", "放火",
        // 色情
        "约炮", "一夜情", "色情", "裸聊", "视频裸",
        // 赌博
        "赌博", "网赌", "博彩", "老虎机",
        // 毒品
        "冰毒", "大麻", "海洛因", "K粉", "摇头丸", "吸毒",
        // 诈骗
        "刷单", "网贷", "套路贷",
        // 违规广告
        "加微信", "加QQ", "扫码", "免费领", "兼职",
    };

    /**
     * 检查文本是否包含违规内容
     * @return null表示通过，否则返回违规提示
     */
    public static String check(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String lower = text.toLowerCase();
        for (String word : BANNED_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                return "内容包含违规信息，请修改后重试";
            }
        }
        return null;
    }
}
