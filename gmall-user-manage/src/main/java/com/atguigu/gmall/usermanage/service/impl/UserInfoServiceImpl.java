package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24*7;
    /**
     * 查找所有用户
     * @return
     */
    @Override
    public List<UserInfo> findAll() {
        List<UserInfo> userInfos = userInfoMapper.selectAll();
        return userInfos;
    }

    /**
     * 根据名字获取用户
     * @return
     */
    @Override
    public UserInfo getUserByName(String name) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",name);
        UserInfo userInfo = userInfoMapper.selectOneByExample(example);
        return userInfo;
    }
    /**
     * 根据名字模糊查询获取用户
     * @return
     */
    @Override
    public List<UserInfo> getUserByNameToLink(String name) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("name","%"+name+"%");
        List<UserInfo> userInfos = userInfoMapper.selectByExample(example);
        return userInfos;
    }
    /**
     * 插入用户
     */
    @Override
    public void insertUser(UserInfo userInfo) {
        userInfoMapper.insertSelective(userInfo);
    }
    /**
     * 根据name修改用户
     * @param userInfo
     */
    @Override
    public void updateByName(UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",userInfo.getName());
        userInfoMapper.updateByExampleSelective(userInfo,example);
    }
    /**
     * 根据id删除用户
     */
    @Override
    public void deleteUserById(String id) {
        userInfoMapper.deleteByPrimaryKey(id);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //将密码加密后查询
        String passwd = userInfo.getPasswd();
        String md5Passwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(md5Passwd);
        //select * from userInfo where userName = ? and passwd = ?
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info!=null){
            //登陆成功,存入redis
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    /**
     * 认证用户是否登录,从redis中查询用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {

            //拼接出redis中的key
            String key = userKey_prefix + userId + userinfoKey_suffix;
            //获取jedis进行操作redis
            Jedis jedis = redisUtil.getJedis();
            String userInfoToJson = jedis.get(key);
            //将获取出的JSON转换为对象
            if(userInfoToJson!=null){
                UserInfo userInfo = JSON.parseObject(userInfoToJson, UserInfo.class);
                return userInfo;
            }else{
                return null;
            }
    }

    /**
     * 根据id获取用户
     * @param userId
     * @return
     */
    @Override
    public UserInfo getUserById(String userId) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        UserInfo userInfo1 = userInfoMapper.selectOne(userInfo);
        return userInfo1;
    }
}
