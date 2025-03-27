package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1.查询优惠卷
//        SeckillVoucher id = iSeckillVoucherService.getById(voucherId);
//        //2.判断秒杀是否开始
//        if (id.getBeginTime().isAfter(LocalDateTime.now())) {
//            // 秒杀尚未开始
//            return Result.fail("秒杀尚未开始");
//        }
//        //3.判断秒杀卷是否结束
//        if (id.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        //4.判断库存是否不足
//        if(id.getStock()<1){
//            return Result.fail("库存不足");
//        }
//        //5.扣减库存
//        Long userId = UserHolder.getUser().getId();
//        //创建锁对象
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        //获取锁
//        boolean isLock = lock.tryLock();
//        //判断是否获取锁成功
//        if (!isLock) {
//            //获取锁失败，直接返回失败或者重试
//            return Result.fail("不允许重复下单");
//        }
//        //获取代理对象
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.creatVoucherOrder(voucherId);
//        }finally {
//            lock.unlock();
//        }
//        //或者在成员变量中直接注入自己，通过注入自己的对象去调用事务
//
//
//    }
@Override
public Result seckillVoucher(Long voucherId) {
    //获取用户
    Long userId = UserHolder.getUser().getId();
    //1.执行lua脚本
    Long result = stringRedisTemplate.execute(
            SECKILL_SCRIPT,
            Collections.emptyList(),
            voucherId.toString(),
            userId.toString()
    );
    //2.判断结果是否为0
    int value = result.intValue();
    if(value !=0) {
        //2.1.不为0，直接返回失败
        return Result.fail(value == 1 ? "库存不足" : "不能重复下单");
    }
    //2.2.为0，把下单信息保存到阻塞队列
    long orderId = redisIdWorker.nextId("order");
    //3.返回订单id
    return Result.ok(orderId);
}
    @Transactional
    public  Result creatVoucherOrder(Long voucherId) {
        //4.1.判断用户是否已经购买
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count() ;
        if(count>0){
            return Result.fail("你买过了哦，只能买一次");
        }
        //5.扣减库存
        boolean voucherId1 = iSeckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
                . gt("stock",0)
                .update();
        if(!voucherId1){
            return Result.fail("库存不足");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        //6.3代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //7.返回订单id
        return Result.ok(orderId);
    }
}
