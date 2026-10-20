(()=>{
 const csrfHeaders=()=>{const pair=document.cookie.split('; ').find(x=>x.startsWith('XSRF-TOKEN='));return pair?{'X-XSRF-TOKEN':decodeURIComponent(pair.substring('XSRF-TOKEN='.length))}:{}};
 const selected=new Map();
 const seats=[...document.querySelectorAll('.seat')];
 const labels=document.getElementById('selectedLabels');
 const seatTotal=document.getElementById('seatTotal');
 const comboTotalLabel=document.getElementById('comboTotalLabel');
 const grandPreview=document.getElementById('grandPreview');
 const holdBtn=document.getElementById('holdBtn');
 const holdNotice=document.getElementById('holdNotice');
 const seatIds=document.getElementById('seatIds');
 const comboQtys=[...document.querySelectorAll('.combo-qty')];
 const comboIds=[...document.querySelectorAll('input[name="comboIds"]')];
 const voucherInput=document.querySelector('input[name="voucherCode"]');
 const stageSeats=document.getElementById('stageSeats');
 const stageCombos=document.getElementById('stageCombos');
 const stagePayment=document.getElementById('stagePayment');
 const progressSeat=document.getElementById('progressSeat');
 const progressCombo=document.getElementById('progressCombo');
 const progressPayment=document.getElementById('progressPayment');
 const seatToast=document.getElementById('seatToast');
 const seatToastText=document.getElementById('seatToastText');
 const closeSeatToastBtn=document.getElementById('closeSeatToastBtn');
 let toastTimeout=null;
 let held=false;
 let countdownTimer=null;
 let holdDeadlineMs=null;
 let serverNowMs=null;
 let clockSampleMs=null;
 let holdGeneration=0;
 let activePage=true;
 let checkingHold=false;
 let lastHoldCheckMs=0;
 const stopCountdown=()=>{if(countdownTimer)clearInterval(countdownTimer);countdownTimer=null;holdGeneration++};
 window.addEventListener('pagehide',()=>{activePage=false;stopCountdown()});
 window.addEventListener('pageshow',event=>{if(event.persisted)location.reload()});
 const money=n=>new Intl.NumberFormat('vi-VN').format(Math.round(n||0))+'đ';
 const selectedSeatTotal=()=>[...selected.values()].reduce((a,x)=>a+x.price,0);
 const comboTotal=()=>comboQtys.reduce((a,x)=>a+(Number(x.dataset.price)||0)*(Number(x.value)||0),0);

 function showSeatError(msg){
  if(seatToast&&seatToastText){
   seatToastText.textContent=msg;
   seatToast.classList.add('show');
   if(toastTimeout)clearTimeout(toastTimeout);
   toastTimeout=setTimeout(()=>{
    clearSeatError();
   },3500);
  }
  if(holdBtn){
   holdBtn.classList.remove('btn-shake');
   void holdBtn.offsetWidth;
   holdBtn.classList.add('btn-shake');
  }
 }
 function clearSeatError(){
  if(toastTimeout){
   clearTimeout(toastTimeout);
   toastTimeout=null;
  }
  if(seatToast)seatToast.classList.remove('show');
  if(holdBtn)holdBtn.classList.remove('btn-shake');
 }
 closeSeatToastBtn?.addEventListener('click',clearSeatError);

 function render(){
  labels.textContent=selected.size?[...selected.values()].map(x=>x.label).join(', '):'Chưa chọn';
  const seatSum=selectedSeatTotal(); const comboSum=comboTotal();
  seatTotal.textContent=money(seatSum); comboTotalLabel.textContent=money(comboSum); grandPreview.textContent=money(seatSum+comboSum);
  if(!held) holdBtn.disabled=!selected.size;
  seatIds.value=[...selected.keys()].join(',');
 }
 function rewardLine(id,label){
  let line=document.getElementById(id);
  if(!line){line=document.createElement('div');line.className='summary-line';line.id=id;line.innerHTML='<span></span><strong></strong>';const total=document.querySelector('.summary-total');total.parentNode.insertBefore(line,total)}
  line.querySelector('span').textContent=label;return line.querySelector('strong');
 }
 async function requestQuote(){
  if(!held||!selected.size)return;
  try{
   const r=await fetch(`/api/showtimes/${window.SHOWTIME_ID}/quote`,{method:'POST',headers:{'Content-Type':'application/json',...csrfHeaders()},body:JSON.stringify({seatIds:[...selected.keys()],comboIds:comboIds.map(x=>Number(x.value)),comboQtys:comboQtys.map(x=>Number(x.value)||0),voucherCode:voucherInput?.value||''})});
   const data=await r.json();if(!r.ok)throw new Error(data.error||'Không thể tính ưu đãi');
   grandPreview.textContent=money(data.totalAmount);
   const voucher=rewardLine('voucherDiscountLine','Voucher');voucher.textContent=data.voucherDiscount?'-'+money(data.voucherDiscount):'0đ';
   const membership=rewardLine('membershipDiscountLine','Rex Rewards');membership.textContent=data.membershipDiscount?'-'+money(data.membershipDiscount):'0đ';
   const points=rewardLine('expectedPointsLine','Điểm dự kiến');points.textContent=(data.expectedPoints||0)+' điểm';
  }catch(e){if(voucherInput?.value)showSeatError(e.message)}
 }
 function showStage(name){
  [stageSeats,stageCombos,stagePayment].forEach(x=>x&&x.classList.remove('active'));
  [progressSeat,progressCombo,progressPayment].forEach(x=>x&&x.classList.remove('active'));
  if(name==='seat'){stageSeats.classList.add('active');progressSeat.classList.add('active')}
  if(name==='combo'){stageCombos.classList.add('active');progressSeat.classList.add('done');progressCombo.classList.add('active')}
  if(name==='payment'){stagePayment.classList.add('active');progressSeat.classList.add('done');progressCombo.classList.add('done');progressPayment.classList.add('active')}
  window.scrollTo({top:0,behavior:'smooth'});
 }
 seats.forEach(btn=>btn.addEventListener('click',()=>{
  if(held)return;
  clearSeatError();
  const id=Number(btn.dataset.id);
  if(selected.has(id)){selected.delete(id);btn.classList.remove('selected')}
  else{selected.set(id,{label:btn.textContent.trim(),price:Number(btn.dataset.price)});btn.classList.add('selected')}
  render();
 }));
 comboQtys.forEach(x=>x.addEventListener('input',()=>{x.value=Math.max(0,Math.min(10,Number(x.value)||0));render()}));
 document.querySelectorAll('.qty-plus').forEach(btn=>btn.addEventListener('click',()=>{const input=btn.parentElement.querySelector('.combo-qty');input.value=Math.min(10,(Number(input.value)||0)+1);input.dispatchEvent(new Event('input'))}));
 document.querySelectorAll('.qty-minus').forEach(btn=>btn.addEventListener('click',()=>{const input=btn.parentElement.querySelector('.combo-qty');input.value=Math.max(0,(Number(input.value)||0)-1);input.dispatchEvent(new Event('input'))}));

 holdBtn.addEventListener('click',async()=>{
  clearSeatError();
  holdBtn.disabled=true; const ids=[...selected.keys()]; const generation=++holdGeneration;
  try{
   const r=await fetch(`/api/showtimes/${window.SHOWTIME_ID}/hold`,{method:'POST',headers:{'Content-Type':'application/json',...csrfHeaders()},body:JSON.stringify({seatIds:ids})});
   const data=await r.json(); if(!r.ok)throw new Error(data.error||'Không thể giữ ghế');
   if(!activePage||generation!==holdGeneration)return;
   held=true; holdBtn.classList.add('d-none'); holdNotice.classList.remove('d-none');
   seats.forEach(b=>{b.disabled=true;if(selected.has(Number(b.dataset.id)))b.classList.add('selected')});
   startCountdown(Number(data.expiresAtEpochMillis),Number(data.serverNowEpochMillis)); render(); showStage('combo');
  }catch(e){if(activePage&&generation===holdGeneration){showSeatError(e.message);holdBtn.disabled=false}}
 });
 document.getElementById('goPayment')?.addEventListener('click',()=>{showStage('payment');requestQuote()});
 voucherInput?.addEventListener('change',requestQuote);
 document.getElementById('backToCombos')?.addEventListener('click',()=>showStage('combo'));
 document.getElementById('backToSeats')?.addEventListener('click',async()=>{
  const ids=[...selected.keys()];
  try{await fetch(`/api/showtimes/${window.SHOWTIME_ID}/release`,{method:'POST',headers:{'Content-Type':'application/json',...csrfHeaders()},body:JSON.stringify({seatIds:ids})})}catch(e){}
  held=false; stopCountdown(); holdDeadlineMs=null; holdNotice.classList.add('d-none');holdBtn.classList.remove('d-none');
  seats.forEach(b=>{
   const id=Number(b.dataset.id);
   const isBooked=b.classList.contains('booked');
   const isOtherHeld=b.classList.contains('held')&&!selected.has(id);
   if(!isBooked&&!isOtherHeld){
    b.disabled=false;
    b.classList.remove('held');
    if(selected.has(id)){
     b.classList.add('selected');
    }
   }
  });
  showStage('seat'); render();
 });
 async function verifyHold(generation){
  if(checkingHold||!activePage||!held||generation!==holdGeneration)return;
  checkingHold=true; lastHoldCheckMs=performance.now();
  try{
   const params=new URLSearchParams({expiresAtEpochMillis:String(holdDeadlineMs)});
   [...selected.keys()].forEach(id=>params.append('seatIds',String(id)));
   const r=await fetch(`/api/showtimes/${window.SHOWTIME_ID}/hold-status?${params}`,{headers:{'Accept':'application/json'}});
   if(!r.ok)throw new Error('Không thể kiểm tra trạng thái giữ ghế');
   const result=await r.json();
   if(!activePage||generation!==holdGeneration)return;
   if(result.active){serverNowMs=Number(result.serverNowEpochMillis);clockSampleMs=performance.now();renderCountdown();}
   else{stopCountdown();showSeatError('Hết thời gian giữ ghế. Vui lòng chọn ghế và đặt vé lại.');
    window.setTimeout(()=>{if(activePage)location.reload()},1200)}
  }catch(_){if(activePage&&generation===holdGeneration)document.getElementById('countdown').textContent='Đang kiểm tra trạng thái';}
  finally{checkingHold=false}
 }
 function renderCountdown(){
  if(!held||!activePage)return;
  const el=document.getElementById('countdown');
  if(!Number.isFinite(holdDeadlineMs)||!Number.isFinite(serverNowMs)){el.textContent='Đang kiểm tra trạng thái';return}
  const remaining=Math.max(0,Math.ceil((holdDeadlineMs-serverNowMs-(performance.now()-clockSampleMs))/1000));
  el.textContent=String(Math.floor(remaining/60)).padStart(2,'0')+':'+String(remaining%60).padStart(2,'0');
  if(remaining===0){el.textContent='Đang kiểm tra trạng thái';
   if(performance.now()-lastHoldCheckMs>=4000)verifyHold(holdGeneration)}
 }
 function startCountdown(deadline,serverNow){
  if(countdownTimer)clearInterval(countdownTimer);
  holdDeadlineMs=deadline;serverNowMs=serverNow;clockSampleMs=performance.now();lastHoldCheckMs=clockSampleMs-4000;
  renderCountdown();countdownTimer=setInterval(renderCountdown,1000);
 }
 try{
  const client=new StompJs.Client({webSocketFactory:()=>new SockJS('/ws'),reconnectDelay:3000});
  client.onConnect=()=>client.subscribe(`/topic/showtimes/${window.SHOWTIME_ID}/seats`,msg=>{
   const u=JSON.parse(msg.body);u.seatIds.forEach(rawId=>{
    const id=Number(rawId);const b=document.querySelector(`.seat[data-id="${id}"]`);if(!b)return;
    if(selected.has(id)&&u.status==='HELD')return;
    b.classList.remove('available','held','booked','selected');b.classList.add(String(u.status).toLowerCase());b.disabled=u.status!=='AVAILABLE';
    if(selected.has(id)&&!held){b.classList.add('selected');b.disabled=false}
    if(u.status!=='AVAILABLE'&&!selected.has(id))selected.delete(id)
   });render()
  });client.activate()
 }catch(e){console.warn('WebSocket unavailable',e)}
 render();
})();
