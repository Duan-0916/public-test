package org.corgi.consumer.sourcedownload.model.repo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.corgi.consumer.sourcedownload.model.BaseDo;
import org.corgi.consumer.sourcedownload.model.repo.enums.RepoStatusEnum;
import org.corgi.consumer.sourcedownload.model.repo.enums.RepoTypeEnum;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author fxt
 * @version : RepoInfoDo.java, v 0.1 2021/12/2 6:41 下午 fxt Exp $
 */
@Data
public class RepoInfoDo extends BaseDo {
    /**
     * git/svn etc
     */
    private RepoTypeEnum repoType;

    /**
     * git or https
     */
    private String repoPath;

    /**
     * groupName%2FrepoName
     */
    private String name;

    /**
     * group name
     */
    private String groupName;

    /**
     * repo name
     */
    private String repoName;

    /**
     * default branch
     */
    private String defaultBranch;

    /**
     * master branch
     */
    private String masterBranch;

    /**
     * description
     */
    private String description;

    /**
     * homepage
     */
    private String homepage;

    /**
     * is archived
     */
    private Boolean archived;

    /**
     * source repo
     */
    private String source;

    /**
     * star count
     */
    private Integer starCount;

    /**
     * star user list json
     */
    private String starUsers;

    /**
     * watch count
     */
    private Integer watchCount;

    /**
     * watch user list json
     */
    private String watchUsers;

    /**
     * fork count
     */
    private Integer forkCount;

    /**
     * fork user list json
     */
    private String forkUsers;

    /**
     * is fork
     */
    private Boolean fork;

    /**
     * network count
     */
    private Integer networkCount;

    /**
     * open issues count
     */
    private Integer openIssuesCount;

    /**
     * topic list json
     */
    private String topics;

    /**
     * licence
     */
    private String licence;

    /**
     * created time
     */
    private Date createdAt;

    /**
     * update time
     */
    private Date updateAt;

    /**
     * crawled time
     */
    private Date crawledAt;

    /**
     * pushed time
     */
    private Date pushedAt;

    /**
     * repo size(KB)
     */
    private long size;

    /**
     * Contribution list json
     */
    private String contributions;

    /**
     * Dependencies and Dependents
     */
    private String dependencyGraph;

    /**
     * Lock for update
     */
    private RepoStatusEnum repoStatus;

    /**
     * main language
     */
    private String language;

    /**
     * language list json
     */
    private String languagesJson;

    /**
     * repo version
     */
    private String version;

    /**
     * 获得非转码仓库名
     *
     * @return
     */
    public String nonAsciiRepoName() {
        return this.name.replace("%2F", "/");
    }

    /**
     * 设置语言列表
     *
     * @param languages
     */
    public void toLanguagesJson(Map<String, Integer> languages) {
        this.languagesJson = new Gson().toJson(languages);
    }

    /**
     * 获得语言列表
     *
     * @return
     */
    public Map<String, Integer> fromLanguagesJson() {
        Map<String, Integer> languageMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject jsonObject = new JsonParser().parse(this.languagesJson).getAsJsonObject();
        if (Objects.nonNull(jsonObject)) {
            jsonObject.keySet().forEach(k -> languageMap.put(k, jsonObject.get(k).getAsInt()));
        }
        return languageMap;
    }

    /**
     * 设置star列表
     *
     * @param users
     */
    public void toStarUsers(List<String> users) {
        this.starUsers = new Gson().toJson(users);
    }

    /**
     * 获得star列表
     *
     * @return
     */
    public List<String> fromStarUsers() {
        return new Gson().fromJson(this.starUsers, new TypeToken<List<String>>(){}.getType());
    }

    /**
     * 设置watch列表
     *
     * @param users
     */
    public void toWatchUsers(List<String> users) {
        this.watchUsers = new Gson().toJson(users);
    }

    /**
     * 获得watch列表
     *
     * @return
     */
    public List<String> fromWatchUsers() {
        return new Gson().fromJson(this.watchUsers, new TypeToken<List<String>>(){}.getType());
    }

    /**
     * 设置fork列表
     *
     * @param users
     */
    public void toForkUsers(List<String> users) {
        this.forkUsers = new Gson().toJson(users);
    }

    /**
     * 获得fork列表
     *
     * @return
     */
    public List<String> fromForkUsers() {
        return new Gson().fromJson(this.forkUsers, new TypeToken<List<String>>(){}.getType());
    }

    /**
     * 设置主题列表
     *
     * @param topics
     */
    public void toTopics(List<String> topics) {
        this.topics = new Gson().toJson(topics);
    }

    /**
     * 获得主题列表
     *
     * @return
     */
    public List<String> fromTopics() {
        return new Gson().fromJson(this.topics, new TypeToken<List<String>>(){}.getType());
    }

    /**
     * 设置贡献者列表
     *
     * @param contributions
     */
    public void toContributions(List<String> contributions) {
        this.contributions = new Gson().toJson(contributions);
    }

    /**
     * 获得贡献者列表
     *
     * @return
     */
    public List<String> fromContributions() {
        return new Gson().fromJson(this.contributions, new TypeToken<List<String>>(){}.getType());
    }
}
