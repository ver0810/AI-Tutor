请基于以下课程资料文本生成测验题，要求严格输出结构化 JSON：

【资料文本】
{studentProfileText}

【目标题量】
总题数：{questionCount}

【题型分布建议】
- 基础概念（CONCEPT）：{conceptCount} 题
- 实践应用（PRACTICE）：{practiceCount} 题
- 深度分析（ANALYSIS）：{analysisCount} 题
- 场景模拟（SCENARIO）：{scenarioCount} 题

【追问配置】
每道主问题最多追问数：{followUpCount}

【历史题目（避免重复）】
{historicalQuestions}

约束：
1. 题目必须围绕“提供的资料文本”进行深度设计，不得空泛。
2. 禁止出现“简历”、“面试官”、“求职”等字眼。
3. category 应为资料中具体模块的标题或知识点，type 必须与分布建议中的大类（CONCEPT, PRACTICE, ANALYSIS, SCENARIO）保持一致。
4. 输出 JSON 结构：{"questions": [{"question": "...", "type": "CONCEPT/PRACTICE/ANALYSIS/SCENARIO", "category": "...", "followUps": ["..."]}]}
5. 输出只包含问题数据，不输出解释性文本。
