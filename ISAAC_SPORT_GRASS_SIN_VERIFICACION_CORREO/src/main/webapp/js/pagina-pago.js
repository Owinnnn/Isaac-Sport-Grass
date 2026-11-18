/* La pagina de cobro es independiente: no crea reservas hasta enviar el voucher. */
(()=>{'use strict';
const $=s=>document.querySelector(s),money=x=>'S/ '+Number(x||0).toFixed(2),p=new URLSearchParams(location.search);
const existing=p.get('id'),court=p.get('court'),date=p.get('date'),time=p.get('time');
let reservationId=existing||null,qrReady=false,number='';
async function get(path){const r=await fetch(path,{credentials:'same-origin',cache:'no-store'}),j=await r.json();if(!r.ok)throw Error(j.message||'Error del servidor');return j;}
async function post(path,form){const opt={method:'POST',credentials:'same-origin',headers:{'X-ISG-Request':'1'}};if(form instanceof FormData){opt.body=form;}else{opt.headers['Content-Type']='application/x-www-form-urlencoded;charset=UTF-8';opt.body=new URLSearchParams(form);}const r=await fetch(path,opt),j=await r.json();if(!r.ok)throw Error(j.message||'No se pudo guardar');return j;}
function fail(t){$('#checkout-content').hidden=true;$('#checkout-error').hidden=false;$('#checkout-error').textContent=t;}
function show(b){$('#checkout-court').textContent=b.court_name;$('#checkout-date').textContent='📅 '+b.booking_date+' · '+String(b.booking_time).slice(0,5)+' (1 hora)';$('#checkout-district').textContent='📍 '+b.court_district+' · '+b.court_sport;$('#checkout-total').textContent=money(b.amount_total);$('#checkout-deposit').textContent=money(b.deposit_amount);$('#checkout-balance').textContent=money(Number(b.amount_total)-Number(b.deposit_amount));$('#checkout-status').textContent=(b.status||'LISTA_PARA_PAGAR').replaceAll('_',' ');$('#checkout-content').hidden=false;}
function done(title,message){$('#checkout-content').hidden=true;$('#checkout-complete').hidden=false;$('#checkout-complete-title').textContent=title;$('#checkout-complete-text').textContent=message;}
function remaining(sec){if(sec===null||sec===undefined)return;const started=Date.now(),label=$('#checkout-time'),button=$('#checkout-submit');function tick(){const left=Math.max(0,Number(sec)-Math.floor((Date.now()-started)/1000));label.textContent=left>0?'Tiempo para adjuntar el voucher: '+Math.floor(left/60)+'m '+String(left%60).padStart(2,'0')+'s':'El plazo de esta separación terminó. Elige otro horario.';if(left===0)button.disabled=true;}tick();setInterval(tick,1000);}
async function load(){try{
 const config=await get('payments/config');number=config.number||'';qrReady=!!config.qrReady;
 $('#checkout-number').textContent=number||'No configurado por el administrador';
 if(existing){if(!/^[1-9][0-9]*$/.test(existing))return fail('Reserva incorrecta.');const b=await get('payments/detail?id='+encodeURIComponent(existing));show(b);if(b.status!=='PENDIENTE_PAGO'){const label=b.status==='EN_REVISION'?'Voucher enviado: pendiente de aprobación':b.status==='CONFIRMADA'?'Reserva confirmada':'Estado de la reserva: '+b.status;done(label,b.rejection_reason||'Consulta tus separaciones desde tu perfil.');return;}remaining(b.seconds_remaining);}
 else {if(!court||!/^[1-9][0-9]*$/.test(court)||!date||!time)return fail('Selecciona primero una cancha, una fecha y un horario.');const courts=await get('api/courts');const c=courts.find(v=>String(v.id)===court&&v.available);if(!c)return fail('La cancha ya no está disponible.');const total=Number(c.price);show({court_name:c.name,court_district:c.district,court_sport:c.sport,booking_date:date,booking_time:time,amount_total:total,deposit_amount:Math.round(total*50)/100,status:'LISTA_PARA_PAGAR'});$('#checkout-time').textContent='El horario se bloqueará durante 20 minutos cuando envíes tu comprobante. Si alguien lo reserva antes, recibirás un aviso.';}
 if(!qrReady||!number){$('#checkout-submit').disabled=true;$('#checkout-feedback').textContent='⚠️ El propietario aún no configuró el QR o número Yape. Comunícate con administración. No realices ningún pago todavía.';$('#checkout-qr').hidden=true;return;}
 $('#checkout-qr').src='payments/qr?v='+Date.now();$('#checkout-qr').onerror=()=>{$('#checkout-submit').disabled=true;$('#checkout-feedback').textContent='No se puede abrir el QR. Contacta al administrador.'};
 }catch(e){fail(e.message);}}
 $('#checkout-proof-form').onsubmit=async ev=>{ev.preventDefault();const btn=$('#checkout-submit');btn.disabled=true;$('#checkout-feedback').textContent='Procesando separación y comprobante…';try{
  const data=new FormData(ev.currentTarget),file=data.get('proof');if(!file||file.size<100||file.size>4194304)throw Error('Adjunta una imagen JPG, PNG o WebP de máximo 4 MB.');
  if(!reservationId){const b=await post('payments/reserve',{court,date,time});reservationId=String(b.id);history.replaceState(null,'','pago.jsp?id='+encodeURIComponent(reservationId));}
  data.append('id',reservationId);await post('payments/proof',data);
  done('✅ Voucher enviado correctamente','🔔 El administrador verá tu solicitud y comprobará el depósito en Yape. Tu cancha solo quedará confirmada cuando apruebe el pago.');
 }catch(e){$('#checkout-feedback').textContent='⚠️ '+e.message+(reservationId?' — Puedes reintentar desde esta misma página.':'');btn.disabled=false;}};
 load();
})();
