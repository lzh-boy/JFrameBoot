package com.jf.service.user;

import com.github.pagehelper.PageInfo;
import com.jf.db2.User2Mapper;
import com.jf.mapper.TestMapper;
import com.jf.mapper.UserMapper;
import com.jf.model.User;
import com.jf.model.custom.IdText;
import com.jf.string.StringUtil;
import com.jf.system.cache.RedisLocker;
import com.jf.system.cache.lock.AquiredLockWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户Service
 * Created by xujunfei on 2016/12/21.
 */
@Service
@Transactional
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Autowired(required = false)
    private User2Mapper user2Mapper;

    @Resource
    private TestMapper testMapper;


    // RedisLocker
    @Autowired(required = false)
    private RedisLocker locker;

    /**
     * 测试Redisson分布式锁
     *
     * @param userId
     */
    public void testLock(Long userId) throws Exception {
        locker.lock("user_" + userId + "_lock", new AquiredLockWorker<Object>() {
            @Override
            public Object invokeAfterLockAquire() throws Exception {

                User user = userMapper.findSimpleById(userId);
                user.setMoney(user.getMoney() - 1);
                userMapper.update(user);

                return null;
            }
        });
    }

    /**
     * 测试msyql cluster集群
     * <p>已安装集群环境</p>
     *
     * @return
     */
    public List<IdText> testMysqlCluster() {
        return testMapper.findAll();
    }

    /**
     * 测试事务回滚 testRollbackA & testRollbackB
     */
    @Transactional(value = "dbTransactionManager")
    public void testRollbackA() {
        User user = userMapper.findById(10000l);
        user.setNickname("db1_rollback");
        userMapper.update(user);
        System.out.println(1 / 0); // error
        user = new User(10001l);
        user.setNickname("db2_rollback2");
        userMapper.update(user);
    }

    @Transactional(value = "db2TransactionManager")
    public void testRollbackB() {
        User user = user2Mapper.findById(10000l);
        user.setNickname("db2_rollback");
        user2Mapper.update(user);
        System.out.println(1 / 0); // error
        user = new User(10001l);
        user.setNickname("db1_rollback2");
        user2Mapper.update(user);
    }

    /**
     * 测试多数据源
     *
     * @param source
     * @return
     */
    public User testMutilSource(String source) {
        User user = null;
        if ("db1".equals(source)) {
            user = userMapper.findById(10000l);
        }
        if ("db2".equals(source)) {
            user = user2Mapper.findById(10000l);
        }
        return user;
    }

    /**
     * 按id查询用户
     * <p>测试Redis缓存</p>
     *
     * @param id
     * @return
     */
    @Cacheable(value = "user", key = "'findUserById'+#id")
    public User findUserById(Long id) {
        return userMapper.findById(id);
    }


    /**
     * 用户更新信息
     * <p>测试Redis缓存-更新会直接删除老数据</p>
     *
     * @param user
     * @return
     */
    @CacheEvict(value = "user", key = "'findUserById'+#user.id")
    public int updateUser(User user) {
        if (StringUtil.isNotBlank(user.getPassword())) {
            user.setPassword(StringUtil.MD5Encode(user.getPassword()));
        }
        return userMapper.update(user);
    }

    /**
     * 用户分页查询
     *
     * @param condition
     * @return
     */
    public PageInfo findUserByPage(User condition) {
        // 需要自定义排序的，必须指定默认排序
        if (StringUtil.isBlank(condition.getPageSort())) {
            condition.setPageSort("u.id DESC");
        }
        condition.setPage(true);
        List<User> list = userMapper.findByCondition(condition);
        return new PageInfo(list);
    }

    /**
     * @param condition
     * @return
     */
    public List<User> findByCondition(User condition) {
        return userMapper.findByCondition(condition);
    }

    /**
     * 通过id查询字段
     * <p>只能在service或controller调用，禁止直接在request传参</p>
     *
     * @param userId
     * @param field
     * @return
     */
    public Object findFieleByUserId(Long userId, String field) {
        return userMapper.findFieleByUserId(userId, field);
    }

    /**
     * 手机号模糊查询
     *
     * @param phone
     * @return
     */
    public List<IdText> findUserLikePhone(String phone) {
        return userMapper.findUserLikePhone(phone);
    }

    /**
     * 按手机号查询
     *
     * @param phone
     * @return
     */
    public User findUserByPhone(String phone) {
        User condition = new User();
        condition.setPhone(phone);
        List<User> list = userMapper.findByCondition(condition);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 按手机号查询总数
     *
     * @param phone
     * @return
     */
    public int findUserCountByPhone(String phone) {
        return userMapper.findCountByKey("phone", phone);
    }

    /**
     * 按邮箱查询总数
     *
     * @param email
     * @return
     */
    public int findUserCountByEmail(String email) {
        return userMapper.findCountByKey("email", email);
    }

    /**
     * 账号密码查询
     *
     * @param account
     * @param password
     * @return
     */
    public User findUserByNameAndPwd(String account, String password) {
        return userMapper.findByNameAndPwd(account, StringUtil.MD5Encode(password));
    }

    /**
     * 新增用户
     *
     * @param nickname
     * @param email
     * @param password
     * @param phone
     * @return
     */
    public int insertUser(String nickname, String email, String password, String phone) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(StringUtil.MD5Encode(password));
        user.setPhone(phone);
        user.setNickname(nickname);
        // 新增用户
        return userMapper.insert(user);
    }

    /**
     * 更新用户头像
     *
     * @param userId
     * @param avatar
     * @return
     */
    public int updateUserAvatar(Long userId, String avatar) {
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatar);
        return userMapper.update(user);
    }

    /**
     * 删除用户(逻辑删除)
     *
     * @param userId
     * @return
     */
    public int deleteUser(Long userId) {
        return userMapper.delete(userId);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    public int deleteBatch(Long[] ids) {
        return userMapper.deleteBatch(ids);
    }

}
