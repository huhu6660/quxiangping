--1.参数列表
--1.1.优惠劵ID
local voucherId = ARGV[1]
--1.2.用户ID
local userId = ARGV[2]

--2.数据key
--2.1.库存key
local stockKey = 'seckill:stock:' .. voucherId
--2.2.订单key
local orderKey = 'seckill:order:' .. voucherId

--3.脚本业务
--3.1.判断库存是否充足
local stock = tonumber(redis.call('get', stockKey)) or 0  -- 处理键不存在的情况
if (stock <= 0) then
    return 1
end
--3.2.判断用户是否已经购买过
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 用户已经购买过
    return 2
end
--3.3.扣减库存
redis.call('incrby', stockKey, -1)
--3.4.记录用户购买记录
redis.call('sadd', orderKey, userId)
return 0