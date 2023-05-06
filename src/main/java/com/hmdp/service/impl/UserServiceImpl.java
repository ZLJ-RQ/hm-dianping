package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)){
            //2.如果不符合,返回错误信息
            return Result.fail("手机号格式有误");
        }
        //3.符合,生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.存储验证码到session
        session.setAttribute("code",code);

        //5.发送验证码
        log.debug("发送短信验证码成功,验证码:{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //2.如果不符合,返回错误信息
            return Result.fail("手机号格式有误");
        }
        //3.校验验证码,从session中获取验证码并校验
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if (cacheCode==null||!cacheCode.equals(code)){
            //4.如果不一致,就报错
            return Result.fail("验证码有误");
        }
        //5.一致,则根据手机号查询用户
        User user = query().eq("phone", phone).one();
        if (user==null){
            //6.用户不存在,创建用户并保存到数据库
            user = createUserWithPhone(phone);
        }
        //7.存储用户信息到session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        //1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        //2.将用户存储到数据库中
        save(user);
        return user;
    }
}
