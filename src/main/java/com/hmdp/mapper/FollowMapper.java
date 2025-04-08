package com.hmdp.mapper;

import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

/**
 * 关注 Mapper接口
 */
public interface FollowMapper extends BaseMapper<Follow> {
    @Delete("delete from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId}")
    Boolean deleteFollow(Long userId, Long followUserId);
}
