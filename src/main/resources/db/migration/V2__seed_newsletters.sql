-- Newsletter 기준 데이터를 개발/운영 DB에 Flyway migration으로 적재한다.
-- email unique key를 기준으로 기존 row가 있으면 seed 값으로 갱신한다.
INSERT INTO newsletter (
    name,
    description,
    image_url,
    category,
    subscribe_url,
    main_page_url,
    email
) VALUES
    ('Daily Byte', '하루 10분, 경제를 읽는 가장 쉬운 방법', 'https://pbs.twimg.com/profile_images/1767482947962347520/avMogr0B_400x400.jpg', 'BIZ', 'https://page.stibee.com/subscriptions/81111', 'https://www.mydailybyte.com/', 'byteteam365@mydailybyte.com'),
    ('긱뉴스', '매주 월요일 아침, 지난 일주일간의 GeekNews 중 엄선한 뉴스들을 이메일로 보내드립니다.', 'https://i.scdn.co/image/ab67656300005f1f53c555152622805b90dd3bce', 'TECH', 'https://news.hada.io/weekly', 'https://news.hada.io/weekly', 'news@hada.io'),
    ('어피티', '매일 아침 나에게 찾아오는 경제뉴스, 한국 경제 뉴스레터 1위 어피티입니다.', 'https://yt3.googleusercontent.com/tTCQRfOWX4Xww8qo3zyEmRGkw2tMA8Vi-O1_C89rCcH39raADiTJiOzWCGD8ej8cbjZ8QZ6H8o4=s900-c-k-c0x00ffffff-no-rj', 'BIZ', 'https://uppity.co.kr/subscription/', 'https://uppity.co.kr/', 'moneyletter@uppity.co.kr'),
    ('디그', '세상 돌아가는 경제 이야기, 하루 5분 <디그>로 편하게 읽어보세요.', 'https://s3.ap-northeast-2.amazonaws.com/img.stibee.com/a82ab710-ca85-4f2e-bda8-3c7abcbad46a.png', 'BIZ', 'https://page.stibee.com/subscriptions/159161', 'https://www.mk.co.kr/newsletter', 'dig@mk.co.kr'),
    ('까탈로그', '어떤 제품이 새로 나왔는지, 어떤 물건을 사면 행복해지는지. 에디터들이 까탈스럽게 골라 메일로 배달해드립니다. 디에디트 채널에 업로드 되는 영상과 리뷰, 신제품 소식을 한 번에 감상하세요.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRWyt5ihmaMYdVj4G1U5YkNksNF8MwiRX31ng&s', 'TREND', 'https://the-edit.co.kr/newsletter', 'https://the-edit.co.kr/newsletter', 'newsletter@the-edit.co.kr'),
    ('캐릿', 'Z세대를 위한 트렌드 브리핑 뉴스레터. MZ세대가 주목하는 라이프스타일과 콘텐츠를 소개합니다.', 'https://yt3.googleusercontent.com/7MOP4i_mWejSgr2wZ_jf1wm4mpgF6_VQwRzUo80TqRNN3ODx4CWo-t4X6Mvgan2Rm15Sd7Tk3Q=s900-c-k-c0x00ffffff-no-rj', 'TREND', 'https://www.careet.net/Subscribe', 'https://www.careet.net/', 'careet@careet.net'),
    ('머니네버슬립', '미국의 경제와 기업, 그리고 주식에 대해 이야기해요. 읽다 보면 어느새 지식이 수북하게 쌓여있을 거에요! 남들과는 다른 지식을 쌓고 싶다면, 머니네버슬립만한 게 없어요😎', 'https://img.stibee.com/a91f6d85-092d-41b0-ba28-a16bf0857990.jpg', 'BIZ', 'https://money.stibee.com/subscribe', 'https://money.stibee.com/', 'snowballlabs.official-gmail.com@send.stibee.com'),
    ('H:730', '쉴 새 없이 쏟아지는 뉴스 더미에서 독자님께  꼭 필요한 뉴스를 골라 배달합니다. 하루를 여는 에디터들의 편지와 함께 일목요연하게 브리핑합니다.', 'https://flexible.img.hani.co.kr/flexible/normal/600/420/imgdb/original/2022/1221/20221221503535.jpg', 'SOCIETY', 'https://page.stibee.com/subscriptions/70653', 'https://page.stibee.com/archives/70653', 'daily@hani.co.kr'),
    ('부딩', '부.알.못 밀레니얼을 위한
구독형 부동산 뉴스레터, 부딩', 'https://scs-phinf.pstatic.net/MjAyMTA1MTRfMjEz/MDAxNjIwOTk1MzEyMTEz.ewIJjTsFY11bpHF207FLLEhrGWOBp1XIXrzCYevzm70g.ZUb9NxDOC4xAkOX7Crm2HKcKogdS1b0Jb3aUyL0Qx98g.JPEG/image%7Cpremium%7Cchannel%7Cbooding%7Chome%7C2021%7C05%7C14%7C1620995312021.jpg?type=nfs200_200', 'BIZ', 'https://www.booding.co/', 'https://www.booding.co/blog', 'everybody@booding.co'),
    ('사이드', '좋아하는 것도 너무 많고, 나만의 어떤 것을 시작해보고 싶고, 이것저것 해보고 싶은게 너무 많아서 고민인 사람을 위한 레터', 'https://cdn.maily.so/202502/1739271556468282.png', 'HOBBY', 'https://maily.so/side?pop=up', 'https://maily.so/side', 'side@sideuniverse.xyz'),
    ('Trend A Word', '오늘 꼭 알아야 하는 트렌드 한 단어!', 'https://cdn.maily.so/maily3a9ea478edb9ab39e568f7a2ef8019141616945993', 'TREND', 'https://maily.so/trendaword', 'https://maily.so/trendaword', 'contact@trendaword.com'),
    ('주말토리', '이번 주말에 뭐 하지?
금요일마다 즐거운 놀 거리를 메일로 받아보세요!', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQQWkWXYOTVTuNozAgg2K-ubLJFBV30iao_fw&s', 'TRAVEL', 'https://page.stibee.com/subscriptions/78183?ref=blog.stibee.com', 'https://joomaltory.com/', 'hello@joomaltory.com'),
    ('목요 뇌과학 뉴스', '최신 뇌과학 뉴스와 연구 사례를 소개합니다.
매주 목요일 밤 10시, 새 소식을 받아보세요!', 'https://img2.stibee.com/d2f6fedd-23b7-437f-abc3-ed0668e58c9e.png', 'TREND', 'https://brainletter.stibee.com/subscribe', 'https://brainletter.stibee.com/', 'news@maumproject.me'),
    ('점선면', '뉴스를 점(사실)/선(맥락)/면(관점)으로 분석해 입체적으로 전합니다. 메일함에서 하루 10분으로 주요 뉴스를 모두 알 수 있습니다.', 'https://img2.stibee.com/64852059-9dbb-4a98-9d23-34e81c47cf3b.png', 'SOCIETY', 'https://page.stibee.com/subscriptions/228606?groupIds=226840', 'https://page.stibee.com/subscriptions/228606?groupIds=226840', 'letter@khan.kr'),
    ('우아한테크', '매월, 우아한형제들의 개발 및 기술 관련 소식을 전해드립니다. 우아한테크세미나와 콘퍼런스, 개발자 교육 프로그램과 스터디, 블로그 하이라이트 등의 소식을 빠르게 알고 싶다면? 지금 구독하세요! 😁', 'https://img.stibee.com/f26ca3fa-87bf-4cb5-b087-1503a3b28b2b.png', 'TECH', 'https://page.stibee.com/subscriptions/391509', 'https://woowahantech.stibee.com/', 'doyeonk@woowahan.com'),
    ('노마드코더', '최신 개발 Dev 뉴스. IT 이슈 등 개발자의 일과 성장에 도움이 되는 정보를 모아 보내드려요. AI 소식, 사이드프로젝트, UIUX, 서비스기획 등 개발 이외의 주제도 다루고 있어요. 노마드코더의 각종 이벤트, 강의 업데이트, 챌린지 소식도 함께 전해요. 매주 금요일, 당신의 메일함으로 찾아갑니다. 강의 추천, 이벤트 정보 등의 광고성 정보가 함께 전송될 수 있습니다.', 'https://yt3.googleusercontent.com/ytc/AIdro_kZGbEvWmB_2CZMcZVcCpjFsiQNVQZEehF8jinP6zlFJ7s=s900-c-k-c0x00ffffff-no-rj', 'TECH', 'https://nomadcoders.us16.list-manage.com/subscribe?u=a99b43453db5050f1f26b2744&id=4313d957c9', 'https://us16.campaign-archive.com/home/?u=a99b43453db5050f1f26b2744&id=4313d957c9', 'lynn@nomadcoders.co'),
    ('밑미레터', '매주 월요일 아침 ''진짜 나''를 찾아가는 사람들의 이야기와 마음을 위로하는 이야기가 당신의 메일함으로 도착합니다. 나의 사소한 고민도 밑미레터에 털어놔보세요.', 'https://s3.ap-northeast-2.amazonaws.com/img.stibee.com/2c07e8c6-3790-4998-8835-d6e8485b6567.png', 'HOBBY', 'https://meetmeletter.stibee.com/subscribe', 'https://www.nicetomeetme.kr/', 'hello@nicetomeetme.kr'),
    ('뭐지 뉴스레터', '개발자라면 기획자라면 디자이너라면 취준생이라면 프리랜서라면 IT에 관심이 조금이라도 있다면 매주 수요일 출근 · 등교 시간에 여러분의 메일함으로 조심히 찾아갈게요.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRD16rGFYStgzsEVrk5gAvxhPyPninsEyW7gQ&s', 'TECH', 'https://moji.or.kr/', 'https://moji.or.kr/', 'newsletter@moji.or.kr'),
    ('북플래터', '도서관이나 서점에서 어떤 책을 읽을지 몰라 헤맨 적이 있나요? 북플래터와 함께 여러분의 취향을 찾아봐요🤗 수많은 책들 사이에서 헤매지 않도록, 다양한 취향의 조각들을 엄선해서 만든 ‘북플래터’. 수많은 책들 중 나의 시선이 머무는 책으로 나만의 북플래터를 만들어보고 싶다면, 책을 통해 나를 더 잘 알고 싶다면, 북플래터와 함께 매주 세 권의 책을 만나보세요!', 'https://s3.ap-northeast-2.amazonaws.com/img.stibee.com/45248_list_112470_subscriptions_header_image.png?v=1648792178', 'CULTURE', 'https://page.stibee.com/subscriptions/112470', 'https://bookplatter.stibee.com/', 'bookplatter.letter-gmail.com@send.stibee.com'),
    ('FE News', 'FE News는 네이버 FE 엔지니어들이 엄선한 양질의 FE 및 주요한 기술 소식들을 큐레이션 해 공유하는 것을 목표로 합니다. 이를 통해 국내 개발자들에게 지식 공유에 대한 가치 인식과 성장에 도움을 주고자 합니다. 📆 매월 첫째 주 수요일, 월 1회 발행 됩니다.', 'https://substackcdn.com/image/fetch/$s_!R7cO!,w_80,h_80,c_fill,f_webp,q_auto:good,fl_progressive:steep,g_auto/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F8a361db4-9ede-4d1a-baf0-c7357acd4e4c_479x479.png', 'TECH', 'https://fenews.substack.com/', 'https://fenews.substack.com/', 'fenews@substack.com'),
    ('미라클레터', '"혁신하는 이들을 위한" 미라클레터입니다. 테크 트렌드, 글로벌 트렌드, 실리콘밸리 현장 소식을 인사이트 있게 담아서, 주3회 발송합니다.', 'https://img2.stibee.com/280db095-90a8-472c-830c-bdc6cfe44ba1.png', 'TREND', 'https://page.stibee.com/subscriptions/33271', 'https://www.mk.co.kr/mirakleai/newsletter', 'miraklelab@mk.co.kr'),
    ('수플레', '단 하나의 음악을 보내드립니다. 아주 짧은 편지와 함께요.', 'https://img.stibee.com/28544_1641128616.png', 'CULTURE', 'https://sooplaylist.stibee.com/subscribe', 'https://sooplaylist.stibee.com/', 'hello@sooplaylist.com'),
    ('하버드비즈니스리뷰', '업무에 최신 경영 지식이 필요하신가요? HBR 최신 아티클을 매주 뉴스레터로 만나보세요!', 'https://upload.wikimedia.org/wikipedia/commons/6/6c/HBR-logo.png', 'BIZ', 'https://page.stibee.com/subscriptions/238483', 'https://www.hbrkorea.com/', 'editor@hbrkorea.com'),
    ('한경 CFO insight', '경영 혁신의 최전선에서 활약하는 
CFO들을 위해 최신 자본시장 소식과 산업계 동향을 
매주 목요일 뉴스레터로 전해 드리겠습니다', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ4osz_Wu856AhcDJJwfPs80cf9fkwQoIEG0A&s', 'BIZ', 'https://page.stibee.com/subscriptions/83403', 'https://www.hankyung.com/newsletter', 'editor@hankyung.com'),
    ('퀘스천퍼데이', '우리는 스스로에 대해 아직 잘 몰라요. 알지 못하면 혼란스럽기 마련이죠. 퀘스천퍼데이는 매일, 나를 향한 질문을 보내드려요. 답하는 건, 여러분의 몫이에요. 꾸준히 나에 대해 생각하다 보면, 어느새 달라진 나를 발견할 거예요. 매일 아침, 질문을 받아보기로 결정하셨나요? 그 소중한 마음 이어갈 수 있도록, 좋은 질문들을 성실히 보내드릴게요.', 'https://img.stibee.com/27e3c9fe-a33c-47e3-a7f7-079a516e70db.png', 'HOBBY', 'https://questionperday.stibee.com/subscribe', 'https://questionperday.stibee.com/', 'contact@questionperday.me'),
    ('조쉬의 뉴스레터', '퀄리티 있는 AI, 비즈니스, 프로덕트 이야기를 들려드려요.', 'https://cdn.maily.so/202410/1728892524889005.png', 'AI', 'https://maily.so/josh?pop=up', 'https://maily.so/josh?pop=up', 'josh@maily.so'),
    ('네이버 D2', '기술과 소통하는 다양한 이야기와 현장의 소리를 전합니다.', 'https://pbs.twimg.com/profile_images/1839220564520538112/nx79wGAB_400x400.png', 'TECH', 'https://d2.naver.com/home', 'https://d2.naver.com/home', 'd2_noreply@navercorp.com'),
    ('디자인플러스', 'Design+는 사회 전반에 걸쳐 변화를 모색하는 디자인 혁신가를 위한 플랫폼입니다. 
디자인프레스와 종합 디자인 매거진 월간 <디자인>이 공동 운영하는 이곳은  디자인 소식을 전할 뿐 아니라 다양한 비즈니스 협업이 일어나는 크리에이티브 생태계를 꿈꿉니다.', 'https://yt3.googleusercontent.com/YcZD96RKwIrw2vv5FzD0GY4dq_0ym1Iu9WWMqBbjWXgCVRxVgNqmNSkBTOaIefIZRNwQTVnszw=w2560-fcrop64=1,00005a57ffffa5a8-k-c0xffffffff-no-nd-rj', 'CULTURE', 'https://design.co.kr/newsletter', 'https://design.co.kr/newsletter', 'dgnhouse@design.co.kr'),
    ('시사IN', '메일함에서 만나는 다정한 친구 일주일에 두 번 찾아갈게요', 'https://www.sisain.co.kr/image/logo/snslogo_20240404115546.png', 'SOCIETY', 'https://www.sisain.co.kr/index.html?editcode=MOBILE_18', 'https://www.sisain.co.kr/index.html?editcode=MOBILE_18', 'editor@sisain.co.kr'),
    ('트렌드라이트', '국내 최대 커머스 버티컬 뉴스레터, 트렌드라이트🥤입니다. 트렌드라이트는 우리가 매일 마주하는
''사고 파는 모든 것''에 대한 이야기를 다룹니다. 소비와 선택 뒤에 숨어 있는 전략과 구조를 쉽고 재미있게 풀어 전해드려요.', 'https://img2.stibee.com/08691aab-7601-415e-a310-149a7cb7834e.png', 'TREND', 'https://page.stibee.com/subscriptions/41037?groupIds=96898', 'https://trendlite.stibee.com', 'editor@trendlite.news'),
    ('NHN Cloud', 'NHN Cloud가 전하는 재미있는 클라우드 이야기', 'https://rlhezzw2e.toastcdn.net/images/opengraph/NHNCloud_OGP.png', 'TECH', 'https://info.nhncloud.com/ready-letter.html', 'https://www.nhncloud.com/kr', 'nhncloud@nhncloud.com'),
    ('브리크 brique', '도시, 공간, 사람을 담습니다. 도시인의 삶을 풍요롭게, 일상에 영감을 주는 공간과 라이프스타일을 전합니다.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQLTyLburKwqYVm_VhKbh6fR9HVh6JAlIaLpg&s', 'LIVING', 'https://page.stibee.com/subscriptions/50582', 'https://page.stibee.com/archives/50582', 'brique153@brique.co'),
    ('씨네웨이', '할리우드 미디어 뉴스 소식을 다루는 항공사 씨네웨이에 오신 걸 환영합니다!', 'https://img.stibee.com/fcea2ae7-3d61-4da5-a9e5-94c6e05c6920.png', 'CULTURE', 'https://cineway.stibee.com/subscribe', 'https://cineway.stibee.com/', 'cineway.kr-gmail.com@send.stibee.com'),
    ('씨샵레터', '씨샵레터는 국내외 음악학과 소리연구 분야의 최신 연구 동향을 소개하는 국내 최초, 국내 유일의 정기 구독 메일링 서비스입니다.', 'https://img.stibee.com/06923e00-2be1-4237-ae9b-22cf110796b6.png', 'CULTURE', 'https://csharpletter.stibee.com/subscribe', 'https://csharpletter.stibee.com/', 'hymrc@hanyang.ac.kr'),
    ('슈톡', '스니커즈 신의 다양한 모습을 개인의 취향으로 기록합니다', 'https://cdn.maily.so/mailyd482cb3fe76d8e26e1e698647d1e4ba61607743849', 'TREND', 'https://maily.so/shoetalk?pop=up', 'https://maily.so/shoetalk', 'news@shoetalk.xyz'),
    ('팁스터', '프로덕트와 관련된 다양한 주제의 콘텐츠를 발행합니다. 구독하기를 클릭하시면 발행 내용을 더 자세히 확인하실 수 있어요. 😉 

팁스터란 정보 제공자라는 의미를 갖고 있으며, 앞으로 더 다양하고 알찬 정보를 전하고자 하는 마음과 의지가 담겨있어요.', 'https://cdn.maily.so/mailydfd3985cf89c8dd5ed873917477ed7c61627967836', 'TECH', 'https://maily.so/tipster?pop=up', 'https://maily.so/tipster', 'tipster@tipster-letter.kr'),
    ('안티 에그', 'The square, Where Editors Live 이곳은 신뢰와 돌봄의 성역으로 이 안에서 모두가 동등한 권리와 의무를 갖는다', 'https://img2.stibee.com/94449ebe-9a23-44f4-8dba-aad4953b0e66.png', 'CULTURE', 'https://antiegg.stibee.com/subscribe', 'https://antiegg.stibee.com/', 'editor@antiegg.kr'),
    ('SOSIC 소식', 'SOSIC은 공간을 둘러싼 폭넓고 깊이감 있는 소식을 전달하는 Weekly Journal 뉴스레터입니다. 공간을 경험하는 모든 구성원들을 위한 놓쳐선 안될 트렌드를 큐레이팅하고, 새로운 관점으로 이슈를 전합니다.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR6EXWMw9LMcDHoAvtK_4WGuUEOX9yOFsMaJQ&s', 'LIVING', 'https://sosicweekly.com/subscribe', 'https://sosicweekly.com/archive', 'sosic.official-gmail.com@send.stibee.com'),
    ('레이디러너', '[ 월간 디자인 트렌드 & 인사이트 by 레이디러너 디자인 리서치 스튜디오 ]
매월 마지막 주 목요일, 한 달 간의 디자인 이슈를 모아 전해드려요.', 'https://cdn.maily.so/202512/1766973951689590.png', 'CULTURE', 'https://maily.so/theladylearner?mid=1zg77k953zq&pop=up', 'https://maily.so/theladylearner', 'newsletter@theladylearner.com'),
    ('VITER', '딜부터 금융 트렌드까지 자본시장의 핵심만-
PE, VC, IPO 등 뉴스톱이 엄선한 돈 흐르는 이야기를 한 눈에 접하세요
매주 화 / 목에 만나요🙋🏻‍♀️', 'https://img2.stibee.com/9ed0647b-a183-4498-8a9f-ab241327be2e.png', 'BIZ', 'https://viter.stibee.com/', 'https://www.newstopkorea.com/', 'viter@thevistapartners.com'),
    ('굿모닝 마이 브랜드', '요즘 브랜드 비즈니스, 너무 복잡하지 않나요?

브랜드 속에 숨은 심리·경제·비즈니스를
쉽고 친근하게 풀어내고,
누구나 이해할 수 있도록 정리했습니다. 🔥

칼럼 한 편만 읽어도
이 주제를 충분히 이해하고,
자연스럽게 이야기할 수 있을 만큼
깊이 있게 담았습니다.

매주 금요일 아침 7시,
메일함에서 가장 먼저 받아보세요.', 'https://img2.stibee.com/157803b0-5468-41d5-aebb-8101e7613bc3.png', 'BIZ', 'https://gmmbrand.stibee.com/subscribe', 'https://gmmbrand.stibee.com/', 'quswlsdn0721-naver.com@send.stibee.com'),
    ('Korean FE Article', '매주 한국어로 번역 혹은 작성된 프론트엔드 글을 전달합니다.', 'https://substackcdn.com/image/fetch/$s_!0VPi!,w_176,h_176,c_fill,f_webp,q_auto:good,fl_progressive:steep/https%3A%2F%2Fbucketeer-e05bbc84-baa3-437e-9518-adb32be77984.s3.amazonaws.com%2Fpublic%2Fimages%2F13b944aa-4b63-4c0e-97f0-23df3c2261a7_1280x1280.png', 'TECH', 'https://kofearticle.substack.com/subscribe?utm_source=email&utm_campaign=email-subscribe&r=26qkvx&next=https%3A%2F%2Fkofearticle.substack.com%2Fp%2Fkorean-fe-article-url&utm_medium=email', 'https://kofearticle.substack.com/about?utm_source=subscribe_email&utm_content=learn_more', 'kofearticle@substack.com'),
    ('문화편의점', '문화편의점은 신선하고 맛있는 문화·예술·콘텐츠 업계의 트렌드를 전달하는 뉴스레터예요. 
요즘 틱톡, 인스타그램, 유튜브 할 것 없이 유행이 정말 빠르게 변화하는 만큼 가끔은 따라가기 벅차다고 느껴지는 순간이 있잖아요. 
그럴 때 간편하게 꺼내 먹을 수 있는 콘텐츠로, 문화편의점 하나면 든든해지는 기분을 느낄 수 있어요.', 'https://cdn.maily.so/202301/1675082728678691.jpeg', 'CULTURE', 'https://maily.so/munhwa.cvs?pop=up', 'https://maily.so/munhwa.cvs', 'munhwa.cvs@maily.so'),
    ('포포레터', '포인트오브뷰가 일상의 창작자에게 건네는 포포레터
뉴스보이 포포가 매월 한 통의 편지를 전해드려요.', 'https://img.stibee.com/e8406b71-86b6-4fb8-95d9-abfbf7d6e788.jpg', 'TREND', 'https://popoletter.stibee.com/subscribe', 'https://popoletter.stibee.com/', 'letter@pointofview.kr'),
    ('1집구석', '<1집구석>은 혼자 살아서 더 즐거운 1인 가구의 나다움으로 완성한 특별한 구석을 들여다봅니다. 
취향이 담긴 공간 스토리, 혼자 살아가는 태도와 방식, 소장 가치 1000% 리세일 아이템까지 오직 1인 가구만을 위한 정보를 전해드려요.', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRn04WUEp-ZuZ5bpIURqRe8JoC2EwNtIy7gvg&s', 'LIVING', 'https://page.stibee.com/subscriptions/278969', 'https://1hows.com/newsletter', '1hows@design.co.kr'),
    ('마키나락스+', '기업들은 AI를 어떻게 활용하고 있을까요? 
답을 찾을 수 있는 AI 뉴스레터를 추천합니다. 

산업 특화 AI(Vertical AI) 기업 마키나락스가 매달 발행하는 📩 마키나락스+뉴스레터는 제조, 국방 등 실제 산업의 성공 사례를 중심으로 전개됩니다. 여기에 산업별 AI 적용 사례, 고객 인터뷰, AI 기술 아티클, 컨퍼런스, 웨비나 등 AI를 실전 경험에서 얻은 인사이트도 공유합니다.', 'https://img.stibee.com/5a3f90fb-ab78-400f-9365-5fbaecd3ad6e.png', 'AI', 'https://makinarocksletter.stibee.com/subscribe', 'https://hubspot.makinarocks.ai/newsletter-archive', 'mrx.marketing@makinarocks.ai'),
    ('Weekly deep daiv.', '인공지능 커뮤니티 deep daiv.에서 매주 수요일 AI 트렌드 소식을 알려드립니다!
- 매주 새로운 AI 소식을 전해드려요.
- 지난주 AI 이슈와 뉴스를 정리해드려요.
- deep daiv. 이야기를 들려드려요.', 'https://img.stibee.com/5061141a-9a43-4dd5-8064-a849383f2dde.png', 'AI', 'https://deepdaiv.stibee.com/subscribe', 'https://deepdaiv.stibee.com/', 'manager@deepdaiv.com'),
    ('뉴본 뉴스레터', '수백, 수천개의 정부지원사업 중 창업 7년 이내 기업에게 꼭 맞는 정부지원사업
"이젠 뉴스레터 하나로 모두 확인해보세요!!"

매주 새벽 신선한 정부지원사업을 안내해드릴게요:D', 'https://img.stibee.com/2f6ac552-777b-453f-8bf2-1119bd6ad1a6.png', 'STARTUP', 'https://newborn.stibee.com/subscribe', 'https://newborn.stibee.com/', 'nb_consulting-naver.com@send.stibee.com'),
    ('이오플래닛', '창업가부터 직장인까지 
스타트업을 탐험하는 사람들은 
매일 이오플래닛에 모인답니다.

이오플래닛에 모인 이야기들을 
매주 이오레터를 통해 만나보세요!

-일주일에 딱 한 번 : 금요일 오전 11시에 만날 수 있어요.
-용기 충전하고 가세요! : 창업, 커리어, 실전 팁까지 알차게 담았어요.
-스타트업에서 일하기: 스타트업씬 정보, 트렌드까지 공유해 드려요.', 'https://img.stibee.com/cad4f195-805f-42b6-baf0-9dc25c93abb6.jpg', 'STARTUP', 'https://page.stibee.com/subscriptions/174446', 'https://eopla.net/', 'yoonhye2345-gmail.com@send.stibee.com'),
    ('마부뉴스', 'SBS 데이터저널리즘팀이 보내는 뉴스레터입니다

하나의 이슈를 데이터와 함께 
깊이 있게 살펴봅니다.', 'https://s3.ap-northeast-2.amazonaws.com/img.stibee.com/5f6b03c4-e6f6-4fce-875b-6cc568e1fb03.png', 'SOCIETY', 'https://mabunews.stibee.com/subscribe', 'https://premium.sbs.co.kr/corner/list/mabunews', 'sbsdjmb-gmail.com@send.stibee.com'),
    ('Toby''s Codex', '현대 프로그래밍 기술과 깊이 있는 엔지니어링 지식을 공유하는 테크니컬 아카이브입니다.', 'https://codex.epril.com/icons/codex-icon.svg', 'TECH', 'https://codex.epril.com/', 'https://codex.epril.com/', 'noreply@codex.epril.com'),
    ('aadc', '엠비언트 아메리카나 디깅 클럽', 'https://cdn.maily.so/202511/1762563221666043.png', 'CULTURE', 'https://maily.so/aadc?pop=up', 'https://maily.so/aadc', 'aadc@maily.so'),
    ('COFFEEPOT', '현업 전문가들이 글로벌 산업의 구조를 읽고, 비즈니스의 맥락과 새로운 관점을 전합니다.

커피팟은 다양한 산업의 주요 이슈를 분석해 매일 뉴스레터를 전해드리고 있습니다. 거시경제부터 테크와 AI, 리테일과 미디어, 그리고 에너지까지 글로벌 산업 이슈의 맥락을 쉽게 짚고, 재밌게 해설합니다.', 'https://s3.ap-northeast-2.amazonaws.com/inno.bucket.live/corp/logo/CP00010474.png', 'BIZ', 'https://page.stibee.com/subscriptions/52057', 'https://coffeepot.me/', 'good@coffeepot.me')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    image_url = VALUES(image_url),
    category = VALUES(category),
    subscribe_url = VALUES(subscribe_url),
    main_page_url = VALUES(main_page_url);
