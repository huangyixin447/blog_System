package com.itheima.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.itheima.dao.ArticleMapper;
import com.itheima.dao.CommentMapper;
import com.itheima.dao.StatisticMapper;
import com.itheima.model.domain.Article;
import com.itheima.model.domain.Statistic;
import com.itheima.service.IArticleService;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Classname ArticleServiceImpl
 * @Description TODO
 * @Date 2019-3-14 9:47
 * @Created by CrazyStone
 */
@Service
@Transactional
public class ArticleServiceImpl implements IArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private StatisticMapper statisticMapper;
    /*注入redis服务*/
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private CommentMapper commentMapper;

    // 分页查询文章列表
    @Override
    public PageInfo<Article> selectArticleWithPage(Integer page, Integer count) {//        开始分页，指定页码和每页的数量
        PageHelper.startPage(page, count);
//        从数据库中查询对应的文章列表
        List<Article> articleList = articleMapper.selectArticleWithPage();
        // 封装文章统计数据
        for (int i = 0; i < articleList.size(); i++) {
            Article article = articleList.get(i);
//            根本id获得点击量和评论量相关内容的实体类
            Statistic statistic = statisticMapper.selectStatisticWithArticleId(article.getId());
//            将article的对应属性进行赋值
            article.setHits(statistic.getHits());
            article.setCommentsNum(statistic.getCommentsNum());
        }
//        将articleList放入PageInfo中生成分页实体
        PageInfo<Article> pageInfo=new PageInfo<>(articleList);

        return pageInfo;
    }

    // 统计前10的热度文章信息
    @Override
    public List<Article> getHeatArticles( ) {
//        筛选出点击数量不为0的文章条数（“SELECT * FROM t_statistic WHERE hits !='0' " +
//            "ORDER BY hits DESC, comments_num DESC”）
//        从大到小排序的
        List<Statistic> list = statisticMapper.getStatistic();
        List<Article> articlelist=new ArrayList<>();
//        新建集合,用于存放前十的文章
//
        for (int i = 0; i < list.size(); i++) {
//            由高到低写入list集合，并且到序号为9时终止循环
            Article article = articleMapper.selectArticleWithId(list.get(i).getArticleId());
            article.setHits(list.get(i).getHits());
            article.setCommentsNum(list.get(i).getCommentsNum());
            articlelist.add(article);
            if(i>=9){
                break;
            }
        }
        return articlelist;
    }

    // 根据id查询单个文章详情，并使用Redis进行缓存管理
    public Article selectArticleWithId(Integer id){
//
//        Article article = null;
////        先尝试从redis中获取对应id的文章，如果没有则开始使用数据进行查询
//        Object o = redisTemplate.opsForValue().get("article_" + id);
//        if(o!=null){
//            article=(Article)o;
//        }else{
//            article = articleMapper.selectArticleWithId(id);
//            if(article!=null){
//                redisTemplate.opsForValue().set("article_" + id,article);
//            }
//        }
//        return article;
        Article article =null;
        Object o = redisTemplate.opsForValue().get("article_" + id);
        if (o!=null){
            article=(Article)o;

        }else {

            article=articleMapper.selectArticleWithId(id);
            if (article!=null) {
                redisTemplate.opsForValue().set("article_" + id, article);
            }
        }
        return  article;


    }

    // 发布文章
    @Override
    public void publish(Article article) {
        // 去除表情
        article.setContent(EmojiParser.parseToAliases(article.getContent()));
        article.setCreated(new Date());
        article.setHits(0);
        article.setCommentsNum(0);
        // 插入文章，同时插入文章统计数据
        articleMapper.publishArticle(article);
        statisticMapper.addStatistic(article);
    }

    // 更新文章
    @Override
    public void updateArticleWithId(Article article) {
        article.setModified(new Date());
        articleMapper.updateArticleWithId(article);
        redisTemplate.delete("article_" + article.getId());
    }

    // 删除文章
    @Override
    public void deleteArticleWithId(int id) {
        // 删除文章的同时，删除对应的缓存
        articleMapper.deleteArticleWithId(id);
        redisTemplate.delete("article_" + id);
        // 同时删除对应文章的统计数据
        statisticMapper.deleteStatisticWithId(id);
        // 同时删除对应文章的评论数据
        commentMapper.deleteCommentWithId(id);
    }

}

