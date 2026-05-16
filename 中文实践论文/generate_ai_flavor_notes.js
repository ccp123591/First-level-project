// 生成"论文 AI 味修改说明"docx
const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, AlignmentType,
        Table, TableRow, TableCell, BorderStyle, WidthType, ShadingType } = require('docx');

const FONT_HEI = 'SimHei';
const FONT_SONG = 'SimSun';

const SZ_TITLE = 32;
const SZ_H1 = 24;
const SZ_H2 = 22;
const SZ_BODY = 21;
const SZ_NOTE = 18;

const t = (text, opts = {}) => new TextRun({
  text,
  font: opts.font || FONT_SONG,
  size: opts.size || SZ_BODY,
  bold: !!opts.bold,
  color: opts.color,
  italics: !!opts.italics,
});

const p = (runs, opts = {}) => new Paragraph({
  alignment: opts.align || AlignmentType.LEFT,
  spacing: { line: 360, before: opts.before || 0, after: opts.after !== undefined ? opts.after : 100 },
  children: Array.isArray(runs) ? runs : [runs],
});

const h1 = (text) => p(t(text, { font: FONT_HEI, size: SZ_H1, bold: true }), { before: 240, after: 120 });

const blank = () => new Paragraph({ children: [new TextRun('')] });

const block = (label, text, opts = {}) => {
  const cell = new TableCell({
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      left: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      right: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
    },
    shading: { fill: opts.fill || 'F8F8F8', type: ShadingType.CLEAR },
    margins: { top: 100, bottom: 100, left: 160, right: 160 },
    width: { size: 9026, type: WidthType.DXA },
    children: [
      new Paragraph({
        spacing: { line: 320, after: 60 },
        children: [t(label, { font: FONT_HEI, size: 19, bold: true, color: opts.labelColor || '595959' })],
      }),
      ...text.split('\n').map(line => new Paragraph({
        spacing: { line: 340, after: 40 },
        children: [t(line, { size: SZ_BODY })],
      })),
    ],
  });
  return new Table({
    width: { size: 9026, type: WidthType.DXA },
    columnWidths: [9026],
    rows: [new TableRow({ children: [cell] })],
  });
};

// ============== 内容 ==============

const children = [];

children.push(p(t('FitCoach 论文 AI 味修改说明', { font: FONT_HEI, size: SZ_TITLE, bold: true }),
  { align: AlignmentType.CENTER, after: 120 }));
children.push(p(t('—— 8 处典型 AI 写作痕迹的去机器化改写', { font: FONT_SONG, size: SZ_BODY, color: '7F7F7F' }),
  { align: AlignmentType.CENTER, after: 240 }));

// 总体说明
children.push(h1('整体诊断'));
children.push(p(t('客观说,你的论文 AI 味处在中等水平,主要原因是:科技论文本来就要求"无情绪、第三人称、被动语态",这会和 AI 写作风格高度重叠。但还有 4 类痕迹是真的能改:', { color: '595959' })));
children.push(p([t('1. ', { bold: true }), t('"在 X 方面"开头狂轰滥炸', { bold: true }), t(' — §2.1 连续 4 段都用"在 X 方面"开头,§3.4、§4.2 也是。这是 AI 模板化最明显的特征。')]));
children.push(p([t('2. ', { bold: true }), t('"较 + 形容词"+"具有一定 X"过量', { bold: true }), t(' — 全文超过 25 处"较好/较低/较为/较直观",10+ 处"具有一定 XX"。)')]));
children.push(p([t('3. ', { bold: true }), t('每段尾部必有"标语式总结句"', { bold: true }), t(' — "X 追求的是 Y,而不是 Z"、"这种 X 能够 Y,从而 Z" — 这是 AI 写作的"段尾收束"惯性。')]));
children.push(p([t('4. ', { bold: true }), t('英文摘要 em-dash 滥用', { bold: true }), t(' — 摘要里 4 处长破折号 "—",这是 GPT/Claude 写英文最典型的指纹之一,降重检测系统会盯这个。')]));
children.push(p(t('下面 8 处按 "改了最显效" 排序。每处给原文 + 改写,直接复制替换。', { color: '7F7F7F' })));
children.push(blank());

// =================== 修改 1 ===================
children.push(h1('修改 1:英文摘要去 em-dash(最重要)'));

children.push(p(t('问题:英文摘要里 4 个 "—"(em-dash)中插。这是 ChatGPT/Claude 写英文论文最典型的标记 — 真人写学术英文极少用 em-dash 中插,通常用括号、冒号或拆句。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文 Abstract:', { bold: true })));
children.push(block('原文(4 处 em-dash)',
'To address the difficulties faced by elderly users in home-based physical exercise — including the lack of on-site guidance, difficulty in maintaining proper training rhythm, and the high operational barrier of complex digital products — this paper designs and implements an intelligent fitness coaching system named FitCoach, based on browser-side human pose recognition. ... A rule-driven multi-dimensional scoring model is further constructed along five dimensions — rhythm, stability, depth, symmetry, and completion rate — and is integrated with text-to-speech feedback, a metronome, and a post-training report ...',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成(em-dash 全部换掉,顺便把英文也改得不那么"中式英语转译"):', { bold: true })));
children.push(block('改成',
'Elderly users often face several difficulties in home-based physical exercise, such as the lack of on-site guidance, difficulty in maintaining proper training rhythm, and the high operational barrier of many digital products. To address these issues, this paper designs and implements an intelligent fitness coaching system, FitCoach, based on browser-side human pose recognition. Using only a standard webcam, the system applies the MediaPipe Pose framework to detect body keypoints directly in the browser. Joint-angle computation combined with a finite state machine is then used to recognize and count seven typical training movements: squat, forward stretch, push-up, lunge, glute bridge, plank and jumping jack. On top of action counting, a rule-driven multi-dimensional scoring model is built along five dimensions (rhythm, stability, depth, symmetry, and completion rate), and is paired with text-to-speech feedback, a metronome, and a post-training report. Together, these modules form a complete training loop covering action selection, real-time recognition, result feedback, local saving and conditional uploading. The system follows a Vue 3 / Spring Boot front-end and back-end architecture, and uses IndexedDB together with a Progressive Web Application (PWA) for offline-first storage and installable deployment. Functional verification shows that under ordinary home-camera conditions the system can complete the entire training workflow stably, and can preserve training records even under weak network or unauthenticated states. The system has a low deployment barrier, gives intuitive feedback, and remains interpretable, providing a workable engineering solution for elderly home-exercise scenarios.',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 2 ===================
children.push(h1('修改 2:§2.1 拆掉"在 X 方面"开头连环'));

children.push(p(t('问题:§2.1 全节用"在姿态识别方面/在应用形态方面/在数据管理方面/在系统实现方面"开头,4 段全是同一模板。这是 AI 写作最容易被人识别的痕迹之一。改写时把这个模板词去掉一半,让段落自然衔接。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§2.1 全节,4 段):', { bold: true })));
children.push(block('原文',
'在姿态识别方面,系统选用 MediaPipe Pose 与 BlazePose 作为浏览器端关键点检测方案。对于本项目而言,该技术的实际配置重点在于:针对不同训练动作选取对应的关键关节区域进行检测……\n\n在应用形态方面,系统采用 PWA 方式实现前端部署。通过服务工作线程缓存基础资源,并结合应用清单文件提供安装入口,系统能够在弱网环境下保持基本可用。与传统需要完整下载安装的客户端相比,这种方式更适合老年用户快速进入训练流程。\n\n在数据管理方面,系统采用 IndexedDB 保存本地训练记录。训练结束后,动作名称、训练次数、评分、时长和训练时间等信息会优先写入本地,满足条件时尝试上传后端。这样既可以减少网络波动带来的数据丢失问题,也能让用户在未登录或离线状态下保留基本训练记录。\n\n在系统实现方面,项目采用 Vue3 与 SpringBoot 的前后端分离架构。前端负责页面交互、训练流程和本地业务逻辑,后端负责用户认证、训练记录和扩展接口支撑。该结构有利于将姿态识别、训练交互与后端业务能力解耦,也为后续系统扩展提供了基础。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成:', { bold: true })));
children.push(block('改成',
'姿态识别采用 MediaPipe Pose 与 BlazePose 在浏览器端完成关键点检测,具体配置上针对不同训练动作选取对应关节区域,详见 §4.2。\n\n前端整体以 PWA 形式部署,借助 Service Worker 缓存基础资源、Web App Manifest 提供安装入口,在弱网甚至断网环境下仍能进入训练流程,降低了老年用户的使用门槛。\n\n训练记录通过 IndexedDB 保存在浏览器本地;训练结束后,动作、次数、评分、时长等信息先写本地,登录且联网时再尝试上传后端,因此用户在未登录或离线时也不会丢失训练记录。\n\n工程上采用 Vue 3 + Spring Boot 的前后端分离架构:前端负责页面交互、训练流程和本地业务,后端负责用户认证、记录管理和扩展接口。这一结构把姿态识别和后端业务能力解耦,也方便后续扩展。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 3 ===================
children.push(h1('修改 3:§4.3 工整 5 分号句拆散'));

children.push(p(t('问题:§4.3 第一段把 5 个评分维度用 5 个并列分号句串起来,每句都是"X 分用于反映/体现 Y"。这种过分整齐的排比是 AI 写作的"对仗冲动",真人写起来很少这么对称。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§4.3 第一段):', { bold: true })));
children.push(block('原文(5 个分号句)',
'……当前评分主要包括节奏、稳定性、深度、对称性和完成率五个维度。其中,节奏分主要反映动作完成时间间隔与目标节拍之间的接近程度;稳定性分用于反映动作过程中的波动情况;深度分用于反映动作幅度是否达到预设要求;对称性分用于反映左右肢体动作的一致性;完成率分则用于体现实际训练次数与目标次数之间的关系。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成(打散对称结构,长短句混合,有些含义合并):', { bold: true })));
children.push(block('改成',
'……当前评分包括 5 个维度。节奏分衡量动作时间间隔与目标节拍的接近程度,稳定性分则反映动作过程中的角度波动幅度,这两项是评价"做得稳不稳"的核心指标。深度分关注动作幅度是否到位,例如蹲得是否够低;对称性分对比左右肢体角度差异,在弓步蹲、俯卧撑等单边发力较明显的动作中尤为重要。完成率分则给出实际次数与目标次数的比值,作为辅助指标。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 4 ===================
children.push(h1('修改 4:删 / 改"具有一定 X"'));

children.push(p(t('问题:全文出现 5 处"具有一定 XX",分布在 §1、§5.1、§5.2、§6.1 等关键位置。这是论文写作里最常见的"敷衍套话",AI 用得也很多。能删就删,不能删就换具体说法。', { color: '595959' })));
children.push(blank());

children.push(p(t('替换清单:', { bold: true })));

const replacements = [
  ['§1 第 3 段', '从而在部署成本、响应速度和隐私保护方面具有一定优势', '从而降低部署成本、提高响应速度,并避免视频数据上传导致的隐私顾虑'],
  ['§5.1', '"在弱网或未登录情况下是否仍可保留基础使用能力" 后面那句不动,但摘要末尾的 "具有一定实际应用基础" 删掉', '直接到 §5.2 不需要再总结一句'],
  ['§5.2 第一段', '步骤较为简洁,具有一定实际应用基础', '步骤简洁,基本满足老年居家场景的使用要求'],
  ['§6.1 第 2 段', '说明浏览器端姿态识别技术在老年居家健身场景中具有一定可行性', '说明浏览器端姿态识别技术在老年居家健身场景中是可行的'],
];

replacements.forEach(([loc, before, after]) => {
  children.push(p([
    t(loc + ': ', { bold: true, color: '595959' }),
    t('"' + before + '" → "' + after + '"', { color: '404040' }),
  ]));
});
children.push(blank());

// =================== 修改 5 ===================
children.push(h1('修改 5:删 / 改"较 + 形容词"过量'));

children.push(p(t('问题:全文 25+ 个"较好/较为/较低/较直观/较稳定/较容易"。论文规范确实需要谨慎措辞,但密度太高就成了 AI 风格。挑下面这些改成肯定句或具体说法。', { color: '595959' })));
children.push(blank());

const jiao = [
  ['摘要(中)', '具有部署门槛低、反馈直观、可解释性强等特点', '保持原句即可,中文摘要里这句已经是肯定句,不用改'],
  ['§1 第 3 段', '为普通摄像头条件下的动作识别提供较稳定的关键点输出', '在普通摄像头条件下也能输出可用的关键点'],
  ['§3.1', '这种总体架构对于老年居家使用场景具有较好的适配性', '(整段建议删,见前一份"冗余修改说明"修改 6)'],
  ['§5.2 第一段', '步骤较为简洁', '步骤简洁(已在修改 4 中处理)'],
  ['§5.3 第一段', '能够帮助用户更直观地理解训练表现', '保留,这里的"直观"不是"较直观",可以不动'],
  ['§6.1 第 1 段', '该系统具有部署门槛较低、训练反馈较直观、弱网环境下仍可保持基础可用等特点', '该系统部署门槛低、训练反馈直观,在弱网环境下仍能保持基础可用'],
];
jiao.forEach(([loc, before, after]) => {
  children.push(p([
    t(loc + ': ', { bold: true, color: '595959' }),
  ]));
  children.push(p(t('原:' + before, { color: 'C00000', size: SZ_NOTE })));
  children.push(p(t('改:' + after, { color: '548235', size: SZ_NOTE })));
});
children.push(blank());

// =================== 修改 6 ===================
children.push(h1('修改 6:打散每段尾部的"标语式总结"'));

children.push(p(t('问题:好几个段落结尾都是 "X 追求的是 Y,而不是 Z" 或 "这种 X 能够 Y,从而 Z" — 像 PPT 总结页。改成不那么对称的收尾。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§4.2 第三段末):', { bold: true })));
children.push(block('原文',
'整体来看,该模块追求的是稳定、可解释、易扩展,而不是复杂的黑箱式判断逻辑。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(p(t('改成:', { bold: true })));
children.push(block('改成',
'相比端到端的黑箱模型,这套规则化判定虽然简单,但每个阈值都能解释清楚,后续扩展新动作也只需要补一组关键点和阈值。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

children.push(p(t('原文(§6.1 第二段末):', { bold: true })));
children.push(block('原文',
'本文的主要工作并不在于提出全新的姿态估计算法,而在于结合实际应用需求,将前端识别、训练反馈和离线优先机制组织成可运行的系统方案,并围绕老年用户的使用特点对低门槛交互和结果可解释性进行了工程化实现。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(p(t('改成:', { bold: true })));
children.push(block('改成',
'本文不在于改进姿态估计算法本身,而在于把前端识别、训练反馈和离线优先存储拼成一个能跑通的系统,并针对老年用户对低门槛和可解释结果的需求做工程取舍。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 7 ===================
children.push(h1('修改 7:删 "对于 X 而言" 几处'));

children.push(p(t('问题:全文出现 5 次"对于 X 而言",高密度堆叠会显得话术化。挑 3 处删掉或改写。', { color: '595959' })));
children.push(blank());

const duiyu = [
  ['§3.2', '对于老年用户而言,该模块不仅要完成动作识别,还要尽量通过提示降低理解难度', '该模块除了完成动作识别外,还要通过提示帮助老年用户理解'],
  ['§4.1 第二段', '对于本项目而言,该模块的重点不在于复杂模型创新,而在于以较低门槛支撑稳定、实时的训练识别过程', '本项目的重点不是模型创新,而是以低门槛支撑稳定、实时的训练识别'],
  ['§5.2 第一段', '对于老年居家用户而言,整个操作流程无需安装额外软件或设备', '(参考前一份说明,这句整体改写过了)'],
];
duiyu.forEach(([loc, before, after]) => {
  children.push(p([t(loc + ': ', { bold: true, color: '595959' })]));
  children.push(p(t('原:' + before, { color: 'C00000', size: SZ_NOTE })));
  children.push(p(t('改:' + after, { color: '548235', size: SZ_NOTE })));
});
children.push(blank());

// =================== 修改 8 ===================
children.push(h1('修改 8:§5.4 / §6.2 中"首先...其次...最后..."模板打散'));

children.push(p(t('问题:§5.4 用 "首先 / 其次 / 系统虽然… / 最后" 串起 4 条不足;§6.2 用 "首先 / 其次 / 在技术层面 / 在应用层面" 串起未来工作。这套模板组合是 AI 写作"补段落"时最爱用的脚手架。打散一下会自然不少。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§5.4):', { bold: true })));
children.push(block('原文',
'虽然 FitCoach 已经完成了训练主流程,但从面向居家老年人长期使用的角度看,系统仍存在一些不足。首先,浏览器端姿态识别对摄像头角度、光照条件和人体遮挡较为敏感,当家庭环境较暗或拍摄位置不合适时,识别稳定性会受到影响。其次,当前评分机制本质上仍是规则驱动的工程化实现,能够提供基础训练反馈,但还不能替代专业康复或健身指导。系统虽然具备本地保存能力,但训练记录与后端之间的自动同步机制仍需进一步完善。最后,本文尚未开展大规模老年用户实测,因此对于适老化交互细节、长期使用体验和用户接受度,还需要在后续工作中继续验证。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成(打散为长短不一的句子,内容不变):', { bold: true })));
children.push(block('改成',
'就当前实现而言,FitCoach 仍存在几方面的局限。最直接的是浏览器端姿态识别对摄像头角度、光照和人体遮挡比较敏感,在家庭光线不足或拍摄位置不合适时,关键点稳定性会受影响。评分部分目前仍是规则驱动的工程化实现,可以给出方向性反馈,但与专业康复或健身指导的差距明显。本地记录与后端的自动同步机制虽然已经具备基础接口,但触发时机、字段契约等还没完全闭环。此外,本文尚未做大规模老年用户实测,因此适老化交互细节、长期使用意愿和用户接受度,都需要后续补充验证。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 总结 ===================
children.push(h1('改完之后大概是什么效果'));

children.push(p(t('做完上面 8 处:', { color: '595959' })));
children.push(p(t('• 英文摘要里的 GPT/Claude 指纹(em-dash)清掉,过 AI 检测会更稳。')));
children.push(p(t('• "在 X 方面" 高频开头从 7 次降到 3 次,§2.1 节读起来不会像表格目录。')));
children.push(p(t('• "较 X / 具有一定 X" 从 30+ 处降到 15 处左右,密度回到正常学术论文水平。')));
children.push(p(t('• "首先/其次/最后" 模板打散一处,留一处即可,不会显得每段都是套模板。')));
children.push(p(t('• 整体读起来更像"一个二级以上学习写作的本科生在讲自己的项目",而不是"AI 在汇报项目"。')));
children.push(blank());

children.push(p(t('补一句心得', { bold: true, font: FONT_HEI })));
children.push(p(t('AI 味的根源不是某个词,而是"句式过于工整 + 抽象名词太多 + 没有具体细节"。最有用的反 AI 化技巧其实是:每写完一段,问自己"这一段里有没有具体的数字、文件名、动作名、参数"——只要每段都至少塞一两个具体的东西(比如 "shouldn\'t < 0.5"、"5 帧滑动均值"、"thresholdDown=90"),AI 味会自然下降一大半。', { color: '595959' })));
children.push(p(t('你的论文里其实已经有具体数字(表 2 的阈值 90/160 等),问题只是这些具体数字没扩散到正文段落。如果时间允许,可以在 §4.2 / §4.3 描述里穿插几个具体数字(比如 "5 帧滑动均值平滑"、"BPM 默认 30"、"自动暂停阈值 150 帧"),效果会更好。', { color: '595959' })));

// =================== 输出 ===================

const doc = new Document({
  styles: { default: { document: { run: { font: FONT_SONG, size: SZ_BODY } } } },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    children,
  }],
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync('D:/备份/一级项目/中文实践论文/论文-AI味修改说明.docx', buf);
  console.log('OK');
});
