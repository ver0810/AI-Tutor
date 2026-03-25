请基于以下课程资料文本生成测验题，要求严格输出结构化 JSON：

【资料文本】
{studentProfileText}

【目标题量】
总题数：{questionCount}

【题型分布（尽量满足）】
- 项目实践题：{projectCount}
- MySQL 题：{mysqlCount}
- Redis 题：{redisCount}
- Java 基础题：{javaBasicCount}
- Java 集合题：{javaCollectionCount}
- Java 并发题：{javaConcurrentCount}
- Spring 题：{springCount}

【追问配置】
每道主问题最多追问数：{followUpCount}

【历史题目（避免重复）】
{historicalQuestions}

约束：
1. 题目必须围绕“课程资料学习”场景，禁止出现简历求职/面试官等内容。
2. 每道题是可回答的具体问题，避免空泛问法。
3. category 与 type 必须一致，type 只允许：PROJECT、MYSQL、REDIS、JAVA_BASIC、JAVA_COLLECTION、JAVA_CONCURRENT、SPRING、SPRING_BOOT。
4. followUps 可为空；若非空，内容需紧扣主问题。
5. 输出只包含问题数据，不输出解释性文本。
