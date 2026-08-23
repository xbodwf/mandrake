package at.xbce.entity;

/**
 * 合并进 Zombie 的鸭子接口：蠹虫骑士的运行时状态。
 */
public interface JockeyExtra {

    /** 是否已经呼叫过一次增援（每只宿主一生一次）。 */
    boolean xbce$hasCalledReinforcements();

    void xbce$markReinforcementsCalled();
}
