package com.wenda.controller;

import com.wenda.Utils.WendaUtils;
import com.wenda.async.EventModel;
import com.wenda.async.EventProducer;
import com.wenda.async.EventType;
import com.wenda.model.Comment;
import com.wenda.model.EntityType;
import com.wenda.model.HostHolder;
import com.wenda.model.Question;
import com.wenda.service.CommentService;
import com.wenda.service.QuestionService;
import com.wenda.service.SensitiveService;
import com.wenda.service.UserService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import java.util.Date;
import java.util.concurrent.DelayQueue;

/**
 * Created by 49540 on 2017/6/28.
 */
@Controller
public class CommentController {
    @Autowired
    CommentService commentService;
    @Autowired
    UserService userService;
    @Autowired
    QuestionService questionService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    SensitiveService sensitiveService;
    @Autowired
    EventProducer eventProducer;

    @RequestMapping(path = "/addComment",method = RequestMethod.POST)
    public String addComment(@RequestParam("questionId") int questionId,
                             @RequestParam("content") String content)
    {
        try {
            Comment comment = new Comment();
            comment.setEntityId(questionId);
            comment.setEntityType(EntityType.TYPE_QUESTION);
            comment.setContent(HtmlUtils.htmlEscape(content));
            comment.setContent(sensitiveService.filter(comment.getContent()));
            if(hostHolder.get()!=null)
            {
                comment.setUserId(hostHolder.get().getId());
            }
            else{
                comment.setUserId(WendaUtils.ANONYMOUS_NAME);
            }
            commentService.addComment(comment);
            int count = commentService.getCommentCount(comment.getEntityId(),comment.getEntityType());
            System.out.println(count);

            //异步产生新鲜事
            eventProducer.fireEvent(new EventModel(EventType.COMMENT).setActorId(comment.getUserId())
                    .setEntityId(questionId));
            questionService.updateCommentCount(questionId,count);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return "redirect:/question/"+questionId;
    }

}
