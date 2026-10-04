// Secrets المطلوبة في Cloudflare: BOT_TOKEN و GEMINI_API_KEY

const BAD_WORDS = ["سخافه", "سخافة", "معفن", "معفّن", "غبي", "انقلع", "تافه"];
const LINK_RE = /(https?:\/\/|www\.|t\.me\/|telegram\.me\/|discord\.gg\/|bit\.ly\/)/i;
const GEMINI_MODEL = "gemini-2.5-flash";

export default {
  // أضفنا ctx هنا لاستخدام waitUntil
  async fetch(request, env, ctx) {
    if (request.method === "GET") return new Response("Caesar AI is running");
    if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });

    try {
      const update = await request.json();
      // استخدام ctx.waitUntil لضمان إكمال المعالجة حتى بعد إرجاع الرد
      ctx.waitUntil(handleUpdate(update, env));
    } catch (error) {
      console.error("Error parsing update:", error);
    }

    // نرجع الرد فوراً لتليغرام حتى لا يعيد المحاولة
    return new Response("OK");
  },
};

async function tg(method, data, env) {
  const response = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/${method}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(data),
  });
  return response.json();
}

async function say(chatId, text, env, replyTo) {
  const data = { chat_id: chatId, text };
  if (replyTo) data.reply_to_message_id = replyTo;
  await tg("sendMessage", data, env);
}

async function admin(chatId, userId, env) {
  try {
    const r = await tg("getChatMember", { chat_id: chatId, user_id: userId }, env);
    return r.ok && ["administrator", "creator"].includes(r.result.status);
  } catch (e) {
    return false;
  }
}

async function askGemini(question, userName, env) {
  const prompt = `أنت قيصر، مساعد ذكي لمجموعة تيليجرام عربية. أجب بالعربية وبأسلوب ودود ومختصر. لا تدّعي أنك إنسان، ولا تقدم نصائح خطرة أو معلومات خاصة. اسم العضو: ${userName}. السؤال: ${question}`;
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${env.GEMINI_API_KEY}`;
  
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
    });
    const data = await response.json();
    return data?.candidates?.[0]?.content?.parts?.[0]?.text || "لم أستطع الإجابة الآن، جرّب بعد قليل.";
  } catch (e) {
    return "حدث خطأ في الاتصال بالذكاء الاصطناعي.";
  }
}

async function handleUpdate(update, env) {
  const m = update.message;
  if (!m || !m.chat || !["group", "supergroup"].includes(m.chat.type) || !m.from) return;
  
  const chatId = m.chat.id;
  const text = m.text || "";
  const cmd = text.trim().split(/\s+/)[0].toLowerCase().split("@")[0];

  // الترحيب بالأعضاء الجدد
  if (m.new_chat_members?.length) {
    const names = m.new_chat_members.map(x => x.first_name).join(", ");
    return say(chatId, `أهلًا ${names}! أنا قيصر AI. اكتب /help لمعرفة الأوامر.`, env, m.message_id);
  }

  // الأوامر الأساسية
  if (cmd === "/start") return say(chatId, "مرحبًا! أنا قيصر AI، مدير ومساعد المجموعة. اكتب /help.", env);
  if (cmd === "/help") return say(chatId, "أوامري:\n/ai سؤالك - اسأل الذكاء الاصطناعي\n/help - المساعدة\n/rules - القوانين\n/warn - تحذير بالرد\n/mute 10 - كتم بالدقائق بالرد\n/unmute - إلغاء الكتم بالرد\n/kick - طرد بالرد", env);
  if (cmd === "/rules") return say(chatId, "القوانين:\n1) الاحترام.\n2) ممنوع الإغراق.\n3) لا تنشر روابط أو إعلانات دون إذن.", env);

  // التحقق من المشرف
  const isAdmin = await admin(chatId, m.from.id, env);

  // الحذف التلقائي للكلمات الممنوعة والروابط
  if (!isAdmin && (LINK_RE.test(text) || BAD_WORDS.some(w => text.toLocaleLowerCase().includes(w.toLocaleLowerCase())))) {
    await tg("deleteMessage", { chat_id: chatId, message_id: m.message_id }, env);
    return say(chatId, `يا ${m.from.first_name || "عضو"}، تم حذف الرسالة لمخالفة القوانين.`, env);
  }

  // الذكاء الاصطناعي (Gemini)
  const bot = await tg("getMe", {}, env);
  const mentioned = bot.ok && text.toLowerCase().includes(`@${bot.result.username.toLowerCase()}`);
  const question = cmd === "/ai" ? text.replace(/^\/ai\s*/i, "").trim() : mentioned ? text.replace(new RegExp(`@${bot.result.username}`, "ig"), "").trim() : "";
  
  if (question) {
    if (!env.GEMINI_API_KEY) return say(chatId, "ميزة AI غير مفعلة بعد: أضف GEMINI_API_KEY في Cloudflare Secrets.", env, m.message_id);
    const answer = await askGemini(question, m.from.first_name || "عضو", env);
    return say(chatId, answer.slice(0, 3900), env, m.message_id);
  }

  // أوامر الإدارة (Warn, Mute, Kick)
  if (["/warn", "/mute", "/unmute", "/kick"].includes(cmd)) {
    if (!isAdmin) return say(chatId, "هذا الأمر للمشرفين فقط.", env);
    const target = m.reply_to_message?.from;
    if (!target) return say(chatId, "استخدم الأمر بالرد على رسالة العضو.", env);
    if (await admin(chatId, target.id, env)) return say(chatId, "لا يمكن تطبيق الأمر على مشرف.", env);
    
    if (cmd === "/warn") return say(chatId, `تم تحذير ${target.first_name}.`, env);
    
    if (cmd === "/kick") {
      await tg("banChatMember", { chat_id: chatId, user_id: target.id }, env);
      await tg("unbanChatMember", { chat_id: chatId, user_id: target.id, only_if_banned: true }, env);
      return say(chatId, `تم طرد ${target.first_name}.`, env);
    }
    
    const mins = Math.max(1, Math.min(parseInt(text.split(/\s+/)[1], 10) || 10, 10080));
    const permissions = cmd === "/mute" ? { can_send_messages: false } : { 
      can_send_messages: true, can_send_audios: true, can_send_documents: true, 
      can_send_photos: true, can_send_videos: true, can_send_video_notes: true, 
      can_send_voice_notes: true, can_send_polls: true, can_send_other_messages: true, 
      can_add_web_page_previews: true 
    };
    
    const data = { chat_id: chatId, user_id: target.id, permissions };
    if (cmd === "/mute") data.until_date = Math.floor(Date.now() / 1000) + mins * 60;
    
    await tg("restrictChatMember", data, env);
    return say(chatId, cmd === "/mute" ? `تم كتم ${target.first_name} لمدة ${mins} دقيقة.` : `تم إلغاء كتم ${target.first_name}.`, env);
  }
}
