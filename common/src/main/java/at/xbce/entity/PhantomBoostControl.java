package at.xbce.entity;

/**
 * 合并进 Phantom$PhantomMoveControl 的鸭子接口：
 * 烟花加速期间临时改写其内部速度上限（模拟鞘翅 + 烟花火箭的推力）。
 */
public interface PhantomBoostControl {

    void xbce$setBoostSpeed(float speed);
}
