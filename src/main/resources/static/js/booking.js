(()=>{
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
 const stageSeats=document.getElementById('stageSeats');
 const stageCombos=document.getElementById('stageCombos');
 const stagePayment=document.getElementById('stagePayment');
 const progressSeat=document.getElementById('progressSeat');
 const progressCombo=document.getElementById('progressCombo');
 const progressPayment=document.getElementById('progressPayment');
 let held=false;
 let countdownTimer=null;
 const money=n=>new Intl.NumberFormat('vi-VN').format(Math.round(n||0))+'đ';
 const selectedSeatTotal=()=>[...selected.values()].reduce((a,x)=>a+x.price,0);
 const comboTotal=()=>comboQtys.reduce((a,x)=>a+(Number(x.dataset.price)||0)*(Number(x.value)||0),0);

 function render(){
  labels.textContent=selected.size?[...selected.values()].map(x=>x.label).join(', '):'Chưa chọn';
  const seatSum=selectedSeatTotal(); const comboSum=comboTotal();
  seatTotal.textContent=money(seatSum); comboTotalLabel.textContent=money(comboSum); grandPreview.textContent=money(seatSum+comboSum);
  if(!held) holdBtn.disabled=!selected.size;
  seatIds.value=[...selected.keys()].join(',');
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
  const id=Number(btn.dataset.id);
  if(selected.has(id)){selected.delete(id);btn.classList.remove('selected')}
  else{selected.set(id,{label:btn.textContent.trim(),price:Number(btn.dataset.price)});btn.classList.add('selected')}
  render();
 }));
 comboQtys.forEach(x=>x.addEventListener('input',()=>{x.value=Math.max(0,Math.min(10,Number(x.value)||0));render()}));
 document.querySelectorAll('.qty-plus').forEach(btn=>btn.addEventListener('click',()=>{const input=btn.parentElement.querySelector('.combo-qty');input.value=Math.min(10,(Number(input.value)||0)+1);input.dispatchEvent(new Event('input'))}));
 document.querySelectorAll('.qty-minus').forEach(btn=>btn.addEventListener('click',()=>{const input=btn.parentElement.querySelector('.combo-qty');input.value=Math.max(0,(Number(input.value)||0)-1);input.dispatchEvent(new Event('input'))}));

 holdBtn.addEventListener('click',async()=>{
  holdBtn.disabled=true; const ids=[...selected.keys()];
  try{
   const r=await fetch(`/api/showtimes/${window.SHOWTIME_ID}/hold`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({seatIds:ids})});
   const data=await r.json(); if(!r.ok)throw new Error(data.error||'Không thể giữ ghế');
   held=true; holdBtn.classList.add('d-none'); holdNotice.classList.remove('d-none');
   seats.forEach(b=>{b.disabled=true;if(selected.has(Number(b.dataset.id)))b.classList.add('selected')});
   startCountdown(300); render(); showStage('combo');
  }catch(e){alert(e.message);holdBtn.disabled=false}
 });
 document.getElementById('goPayment')?.addEventListener('click',()=>showStage('payment'));
 document.getElementById('backToCombos')?.addEventListener('click',()=>showStage('combo'));
 document.getElementById('backToSeats')?.addEventListener('click',async()=>{
  const ids=[...selected.keys()];
  try{await fetch(`/api/showtimes/${window.SHOWTIME_ID}/release`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({seatIds:ids})})}catch(e){}
  held=false; if(countdownTimer)clearInterval(countdownTimer); holdNotice.classList.add('d-none');holdBtn.classList.remove('d-none');
  seats.forEach(b=>{if(selected.has(Number(b.dataset.id))){b.disabled=false;b.classList.remove('held');b.classList.add('selected')}});
  showStage('seat'); render();
 });
 function startCountdown(sec){
  const el=document.getElementById('countdown'); if(countdownTimer)clearInterval(countdownTimer);
  countdownTimer=setInterval(()=>{sec--;el.textContent=String(Math.floor(sec/60)).padStart(2,'0')+':'+String(sec%60).padStart(2,'0');if(sec<=0){clearInterval(countdownTimer);location.reload()}},1000)
 }
 try{
  const client=new StompJs.Client({webSocketFactory:()=>new SockJS('/ws'),reconnectDelay:3000});
  client.onConnect=()=>client.subscribe(`/topic/showtimes/${window.SHOWTIME_ID}/seats`,msg=>{
   const u=JSON.parse(msg.body);u.seatIds.forEach(rawId=>{
    const id=Number(rawId);const b=document.querySelector(`.seat[data-id="${id}"]`);if(!b)return;
    if(selected.has(id)&&u.status==='HELD')return;
    b.classList.remove('available','held','booked','selected');b.classList.add(String(u.status).toLowerCase());b.disabled=u.status!=='AVAILABLE';
    if(u.status!=='AVAILABLE')selected.delete(id)
   });render()
  });client.activate()
 }catch(e){console.warn('WebSocket unavailable',e)}
 render();
})();
