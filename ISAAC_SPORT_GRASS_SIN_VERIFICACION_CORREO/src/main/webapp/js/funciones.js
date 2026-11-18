/* ISAAC SPORT GRASS - cliente de API Java Servlet + MySQL */
(()=>{'use strict';
const $=(s)=>document.querySelector(s), $$=(s)=>[...document.querySelectorAll(s)];
const safe=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money=x=>'S/ '+Number(x||0).toFixed(2);const img=x=>'img/'+encodeURIComponent(x||'cancha-futbol.svg');
const toast=(s,bad=false)=>{let t=$('#toasts');if(t){let d=document.createElement('div');d.className='toast'+(bad?' error':'');d.textContent=s;t.append(d);setTimeout(()=>d.remove(),5000);}else alert(s);};
const params=data=>new URLSearchParams(data);
async function api(url,body){let opt={credentials:'same-origin'};if(body!==undefined){opt.method='POST';opt.headers={'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'};opt.body=params(body);}let r=await fetch('api/'+url,opt),x=await r.json();if(!r.ok)throw Error(x.message||'Error de servidor');return x;}
const error=e=>toast(e.message||'No hay conexión al servidor',true);
$$('[data-toggle-password]').forEach(b=>b.addEventListener('click',()=>{let i=$('#'+b.dataset.togglePassword);i.type=i.type==='password'?'text':'password';}));
const mm=$('[data-mobile-menu]');if(mm)mm.onclick=()=>$('#mobile-nav')?.classList.toggle('open');
const page=document.body.dataset.page;
if(page==='login'){
 let role=new URLSearchParams(location.search).get('rol')==='admin'?'admin':'usuario';
 function selectRole(){$$('.role-tab').forEach(b=>{b.classList.toggle('active',b.dataset.role===role);b.setAttribute('aria-selected',b.dataset.role===role?'true':'false');});}
 $$('[data-role]').forEach(b=>b.onclick=()=>{role=b.dataset.role;selectRole();});selectRole();
 $('#login-form').onsubmit=async e=>{e.preventDefault();let er=$('#auth-error');er.hidden=true;try{let x=await api('login',{email:$('#login-email').value,password:$('#login-password').value,role});location.href=x.role==='ADMIN'?'admin.jsp':'usuario.jsp';}catch(ex){er.textContent=ex.message;er.hidden=false;}};

}
if(page==='register'){
 const captcha=async()=>{try{let d=await api('captcha');$('#captcha-question').textContent=d.question;$('#captcha-answer').value='';}catch(e){error(e);}};
 $('#refresh-captcha').onclick=captcha;captcha();
 $('#register-form').onsubmit=async e=>{e.preventDefault();let er=$('#register-error');er.hidden=true;let pass=$('#register-password').value;if(pass!==$('#register-confirm').value){er.hidden=false;er.textContent='Las contraseñas no coinciden';return;}
 try{let d=await api('register',{name:$('#register-name').value,email:$('#register-email').value,password:pass,captcha:$('#captcha-answer').value});$('#register-success').hidden=false;$('#register-success').style.color='#0a652c';$('#register-success').textContent=d.message;$('#register-form').reset();captcha();toast(d.message);}catch(e){er.hidden=false;er.textContent=e.message;captcha();}};
}
const PAGE_NAV=(selector,prefix)=>{$$(selector).forEach(b=>b.addEventListener('click',()=>{let sec=b.dataset.section||b.dataset.adminSection;$$('[id^="'+prefix+'-"]').forEach(el=>{if(el.tagName==='SECTION')el.hidden=el.id!==prefix+'-'+sec;});$$(selector).forEach(el=>el.classList.toggle('active',(el.dataset.section||el.dataset.adminSection)===sec));}));};
const slots=['07:00','08:00','09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00'];
if(page==='usuario'){
 let courts=[],products=[],cart=[],selected=null,hour=null,category='Todos';
 PAGE_NAV('[data-section]','section');
 const today=()=>{let fmt=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'});return fmt.format(new Date());};
 api('session').then(s=>{if(!s.loggedIn||s.role!=='USER')location.href='login.jsp';else{$('#welcome-name').textContent=s.name.split(' ')[0];$('#nav-username').textContent=s.name;$('#user-avatar').textContent=s.name.slice(0,2).toUpperCase();}}).catch(error);
 const refresh=async()=>{try{[courts,products]=await Promise.all([api('courts'),api('products')]);renderCourts();renderProducts();}catch(e){error(e);}};
 function renderCourts(){let q=($('#filter-search').value||'').toLowerCase(),sp=$('#filter-sport').value,dis=$('#filter-district').value;let list=courts.filter(c=>c.available&&(sp==='todos'||c.sport===sp)&&(dis==='todos'||c.district===dis)&&(c.name.toLowerCase().includes(q)||c.district.toLowerCase().includes(q)));$('#court-count').textContent=list.length+' canchas';$('#courts-empty').hidden=!!list.length;
 $('#courts-grid').innerHTML=list.map(c=>`<article class="court-card" style="overflow:hidden;border-radius:19px;background:#fff;box-shadow:0 7px 25px #0001"><img src="${img(c.image)}" style="width:100%;height:180px;object-fit:cover" alt="Cancha"><div style="padding:22px"><span class="eyebrow text-green">${safe(c.sport)}</span><h3>${safe(c.name)}</h3><p>📍 ${safe(c.district)} · Disponible</p><div style="display:flex;align-items:center;justify-content:space-between;gap:12px"><strong>${money(c.price)} / hora</strong><button class="btn btn-lime" data-book="${c.id}">Separar · Yape 50%</button></div></div></article>`).join('');$$('[data-book]').forEach(b=>b.onclick=()=>openBooking(Number(b.dataset.book)));}
 ['#filter-search','#filter-sport','#filter-district'].forEach(id=>$(id).addEventListener('input',renderCourts));
 const reset=()=>{$('#filter-search').value='';$('#filter-sport').value='todos';$('#filter-district').value='todos';renderCourts();};$('#reset-filters').onclick=reset;$('#empty-reset').onclick=reset;
 const closeBooking=()=>$('#booking-modal').hidden=true;$$('[data-close-modal]').forEach(b=>b.onclick=closeBooking);
 function openBooking(id){selected=courts.find(x=>Number(x.id)===id);if(!selected)return;hour=null;$('#booking-modal').hidden=false;$('#booking-modal-title').textContent=selected.name;$('#modal-court-detail').textContent=selected.sport+' · '+selected.district;$('#modal-court-preview').innerHTML=`<img src="${img(selected.image)}" alt="Cancha" style="width:100%;max-height:180px;object-fit:cover">`;$('#reserve-total').textContent=money(selected.price);$('#reserve-date').min=today();$('#reserve-date').value=today();loadSlots();}
 async function loadSlots(){if(!selected)return;hour=null;$('#confirm-booking').disabled=true;try{let booked=(await api('slots?'+params({court:selected.id,date:$('#reserve-date').value}))).map(x=>x.hour);let currentTime=new Intl.DateTimeFormat('en-GB',{timeZone:'America/Lima',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date());$('#reserve-time-slots').innerHTML=slots.map(s=>{let unavailable=booked.includes(s)||($('#reserve-date').value===today()&&s<=currentTime);return `<button type="button" class="${unavailable?'slot-disabled':'time-slot'}" ${unavailable?'disabled':''} data-hour="${s}" style="border:1px solid #a5b6aa;border-radius:8px;padding:9px;margin:4px;background:${unavailable?'#e0e0e0':'white'}">${s}</button>`;}).join('');$$('[data-hour]').forEach(b=>b.onclick=()=>{hour=b.dataset.hour;$$('[data-hour]').forEach(x=>x.style.background=x===b?'#b8f36b':'white');$('#confirm-booking').disabled=false;});}catch(e){error(e);}}
 $('#reserve-date').onchange=loadSlots;$('#confirm-booking').onclick=()=>{if(!selected||!hour)return toast('Selecciona un horario.',true);location.href='pago.jsp?'+params({court:selected.id,date:$('#reserve-date').value,time:hour});};
 async function loadBookings(){try{let data=await api('bookings');$('#my-count').textContent=data.length;$('#next-booking').textContent=data.find(x=>x.status==='CONFIRMADA')?.booking_date||'Por definir';$('#bookings-empty').hidden=!!data.length;$('#my-reservations').innerHTML=data.map(b=>`<div class="admin-panel" style="padding:20px;margin-bottom:10px"><h3>${safe(b.court_name)}</h3><p>${safe(b.booking_date)} · ${safe(String(b.booking_time).slice(0,5))} · ${safe(b.status)} · ${money(b.price)}</p>${b.status==='CONFIRMADA'?`<button class="btn btn-outline" data-cancel="${b.id}">Cancelar reserva</button>`:''}</div>`).join('');$$('[data-cancel]').forEach(b=>b.onclick=async()=>{if(!confirm('¿Cancelar esta reserva?'))return;try{toast((await api('cancel',{id:b.dataset.cancel})).message);loadBookings();}catch(e){error(e);}});}catch(e){error(e);}}
 function renderProducts(){let list=products.filter(p=>category==='Todos'||p.category===category);$('#product-count').textContent=list.length+' productos';$('#products-grid').innerHTML=list.map(p=>`<article class="court-card" style="padding:18px;background:white;border-radius:18px"><img src="${img(p.image)}" alt="Producto" style="width:100%;height:180px;object-fit:contain"><span class="eyebrow text-green">${safe(p.category)}</span><h3>${safe(p.name)}</h3><p>${money(p.price)} · Stock ${p.stock}</p><button class="btn btn-lime" ${p.stock<1?'disabled':''} data-add="${p.id}">Añadir al carrito</button></article>`).join('');$$('[data-add]').forEach(b=>b.onclick=()=>{let p=products.find(x=>Number(x.id)===Number(b.dataset.add)),i=cart.find(x=>x.id===p.id);if(i){if(i.quantity>=p.stock)return toast('Sin stock suficiente',true);i.quantity++;}else cart.push({id:p.id,quantity:1});renderCart();toast('Producto añadido al carrito');});}
 $$('[data-product-filter]').forEach(b=>b.onclick=()=>{category=b.dataset.productFilter;$$('[data-product-filter]').forEach(x=>x.classList.toggle('active',x===b));renderProducts();});
 const openCart=()=>{$('#cart-drawer').classList.add('open');$('#cart-drawer').setAttribute('aria-hidden','false');$('#cart-overlay').hidden=false;};const closeCart=()=>{$('#cart-drawer').classList.remove('open');$('#cart-drawer').setAttribute('aria-hidden','true');$('#cart-overlay').hidden=true;};$('#open-cart').onclick=openCart;$('#close-cart').onclick=closeCart;$('#cart-overlay').onclick=closeCart;
 function renderCart(){let count=cart.reduce((a,b)=>a+b.quantity,0),total=cart.reduce((s,x)=>s+(Number(products.find(p=>p.id===x.id)?.price)||0)*x.quantity,0);$('#cart-count').hidden=!count;$('#cart-count').textContent=count;$('#cart-subtotal').textContent=money(total);$('#cart-items').innerHTML=cart.length?cart.map(x=>{let p=products.find(i=>i.id===x.id);return `<div style="padding:14px;border-bottom:1px solid #ddd"><strong>${safe(p?.name)}</strong><p>${x.quantity} × ${money(p?.price)} <button data-remove="${x.id}" style="float:right">Quitar</button></p></div>`;}).join(''):'Tu carrito está vacío';$$('[data-remove]').forEach(b=>b.onclick=()=>{cart=cart.filter(x=>String(x.id)!==b.dataset.remove);renderCart();});}
 $('#checkout-btn').onclick=async()=>{if(!cart.length)return toast('Agrega productos al carrito',true);try{let d=await api('checkout',{items:JSON.stringify(cart)});toast(d.message);alert(d.message+'\nNo se ha realizado ningún cobro.');cart=[];renderCart();closeCart();refresh();}catch(e){error(e);}};
 $('#logout-user').onclick=async()=>{await api('logout',{});location.href='login.jsp';};
 refresh();loadBookings();renderCart();
 // Soporte real: mensajes almacenados en MySQL y respuesta desde administración.
 const widget=document.createElement('div');widget.innerHTML=`<button class="support-toggle" id="support-open">💬 Soporte técnico</button><div class="support-window" id="support-window" hidden><div class="support-head"><strong>💬 Soporte · Isaac Sport Grass</strong><button id="support-close" aria-label="Cerrar">×</button></div><p class="support-help">Escribe tu consulta. Un administrador podrá responder.</p><div class="support-feed" id="support-feed"></div><form class="support-send" id="support-form"><input id="support-message" maxlength="1000" placeholder="Escribe tu mensaje..." required><button>Enviar</button></form></div>`;document.body.append(widget);
 let supportOpen=false;
 async function loadSupport(){
   if(!supportOpen)return;
   try{
     const data=await api('support');
     $('#support-feed').innerHTML=data.reverse().map(m=>{
       const mensaje=String(m.message||'');
       const esUsuario=m.author==='USER';
       const confirmado=!esUsuario&&/reserva.*(confirmada|aprobada)|pago.*(aprobado|verificado)/i.test(mensaje);
       const rechazado=!esUsuario&&/(reserva|comprobante|pago).*rechazad[oa]/i.test(mensaje);
       const titulo=confirmado?'✅ Reserva confirmada':rechazado?'❌ Comprobante rechazado':esUsuario?'Tú':'Soporte';
       const borde=confirmado?'border-left:4px solid #22c55e;':rechazado?'border-left:4px solid #ef4444;':'';
       return `<div class="support-bubble ${esUsuario?'mine':''}" style="${borde}"><strong>${titulo}:</strong><div style="white-space:pre-line;margin-top:6px">${safe(mensaje)}</div></div>`;
     }).join('')||'<p>No hay mensajes todavía. ¿En qué podemos ayudarte?</p>';
     $('#support-feed').scrollTop=$('#support-feed').scrollHeight;
   }catch(e){error(e);}
 }
 $('#support-open').onclick=()=>{supportOpen=!supportOpen;$('#support-window').hidden=!supportOpen;if(supportOpen)loadSupport();};
 $('#support-close').onclick=()=>{supportOpen=false;$('#support-window').hidden=true;};
 $('#support-form').onsubmit=async e=>{e.preventDefault();try{toast((await api('support/send',{message:$('#support-message').value})).message);$('#support-message').value='';loadSupport();}catch(e){error(e);}};
 setInterval(loadSupport,12000);
}
if(page==='admin'){
 PAGE_NAV('[data-admin-section]','admin-section');let data=null;
 $('#admin-today').textContent=new Date().toLocaleDateString('es-PE',{dateStyle:'long'});
 const cell=x=>`<td>${safe(x)}</td>`;
 async function refresh(){try{data=await api('admin/data');let C=data.courts,R=data.bookings,P=data.products;
 $('#admin-stats').innerHTML=[['Canchas',C.length],['Reservas',R.length],['Productos',P.length],['Mensajes de soporte',data.support.length]].map(([name,value])=>`<div class="admin-panel" style="padding:24px"><small>${name}</small><h2>${value}</h2></div>`).join('');$('#sidebar-bookings').textContent=R.length;
 $('#admin-court-label').textContent=C.length+' canchas registradas';$('#admin-courts-table').innerHTML=C.map(c=>`<tr>${cell(c.name)}${cell(c.sport)}${cell(c.district)}${cell(money(c.price))}${cell(c.available?'Disponible':'Inactiva')}<td><button class="btn btn-outline" data-toggle="${c.id}">${c.available?'Desactivar':'Activar'}</button></td></tr>`).join('');
 $('#recent-reservations').innerHTML=R.slice(0,5).map(r=>`<tr>${cell(r.court_name)}${cell(r.user_name)}${cell(r.booking_date)}${cell('—')}${cell(r.status)}</tr>`).join('');$('#admin-reservations-table').innerHTML=R.map(r=>`<tr>${cell(r.id)}${cell(r.court_name)}${cell(r.user_name)}${cell(r.booking_date+' '+String(r.booking_time).slice(0,5))}${cell('—')}${cell(r.status)}${cell('Gestionado por cliente')}</tr>`).join('');
 $('#inventory-total').textContent=P.reduce((a,p)=>a+Number(p.stock),0)+' artículos';$('#admin-products-table').innerHTML=P.map(p=>`<tr>${cell(p.name)}${cell(p.category)}${cell(money(p.price))}${cell(p.stock>0?'En stock':'Agotado')}<td><button data-stock="${p.id}" data-delta="-1">−</button> &nbsp;${p.stock}&nbsp; <button data-stock="${p.id}" data-delta="1">+</button></td></tr>`).join('');
 $$('[data-toggle]').forEach(b=>b.onclick=async()=>{try{toast((await api('admin/court/toggle',{id:b.dataset.toggle})).message);refresh();}catch(e){error(e);}});
 $$('[data-stock]').forEach(b=>b.onclick=async()=>{let p=P.find(x=>String(x.id)===b.dataset.stock);try{toast((await api('admin/product/stock',{id:p.id,stock:Math.max(0,Number(p.stock)+Number(b.dataset.delta))})).message);refresh();}catch(e){error(e);}});
 renderSupport();
 }catch(e){error(e);}}
 function renderSupport(){let host=$('#admin-support-panel');if(!host){host=document.createElement('div');host.id='admin-support-panel';host.className='admin-panel';host.style.cssText='padding:22px;margin-top:22px';$('#admin-section-dashboard').append(host);}let messages=data.support||[];host.innerHTML='<h3>💬 Soporte técnico — mensajes recientes</h3><p>Responde a consultas de usuarios registrados.</p>'+messages.filter(m=>m.author==='USER').slice(0,15).map(m=>`<div style="padding:12px;border-bottom:1px solid #ddd"><strong>${safe(m.user_name)}</strong> · ${safe(m.email)}<p>${safe(m.message)}</p><form class="support-admin-reply" data-user="${m.user_id}"><input class="standard-input" name="message" maxlength="1000" required placeholder="Responder a este cliente..."><button class="btn btn-lime" style="margin-top:7px">Responder</button></form></div>`).join('')||'<p>No hay consultas todavía.</p>';$$('.support-admin-reply').forEach(f=>f.onsubmit=async e=>{e.preventDefault();try{toast((await api('admin/reply',{user:f.dataset.user,message:f.elements.message.value})).message);refresh();}catch(e){error(e);}});}
 $('#add-court-btn').onclick=()=>$('#new-court-modal').hidden=false;$$('[data-close-modal]').forEach(b=>b.onclick=()=>$('#new-court-modal').hidden=true);
 $('#new-court-form').onsubmit=async e=>{e.preventDefault();try{toast((await api('admin/court',{name:$('#new-court-name').value,sport:$('#new-court-sport').value,district:$('#new-court-district').value,price:$('#new-court-price').value})).message);$('#new-court-modal').hidden=true;$('#new-court-form').reset();refresh();}catch(e){error(e);}};
 $('#logout-admin').onclick=async()=>{await api('logout',{});location.href='login.jsp';};$('#admin-menu-btn').onclick=()=>$('#admin-sidebar').classList.toggle('open');
 refresh();setInterval(refresh,20000);
}
})();
