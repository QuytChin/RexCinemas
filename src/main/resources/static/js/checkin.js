(()=>{
 const result=document.getElementById('scanResult');
 const manual=document.getElementById('manualCode');
 const manualBtn=document.getElementById('manualScanBtn');
 let busy=false,lastCode='',lastAt=0;
 const esc=s=>String(s??'—').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));
 async function submit(code){
  code=(code||'').trim(); if(!code||busy)return;
  const now=Date.now(); if(code===lastCode&&now-lastAt<2500)return; lastCode=code;lastAt=now;busy=true;
  result.className='scan-result loading'; result.innerHTML='<div class="scan-icon">...</div><h3>Đang kiểm tra vé</h3><p>Vui lòng giữ nguyên mã QR.</p>';
  try{
   const r=await fetch('/admin/checkin/api/scan',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({code})});
   const d=await r.json();
   const ok=!!d.success;
   result.className='scan-result '+(ok?'success':'danger');
   result.innerHTML=`<div class="scan-icon">${ok?'✓':'!'}</div><p class="eyebrow mb-1">${esc(d.status)}</p><h3>${esc(d.message)}</h3>${d.bookingCode?`<div class="scan-ticket-info"><div><span>Mã vé</span><b>${esc(d.bookingCode)}</b></div><div><span>Phim</span><b>${esc(d.movieTitle)}</b></div><div><span>Khách</span><b>${esc(d.customerName)}</b></div><div><span>Suất</span><b>${esc(d.showtime)}</b></div><div><span>Rạp</span><b>${esc(d.cinemaName)} / ${esc(d.auditoriumName)}</b></div><div><span>Ghế</span><b>${esc(d.seats)}</b></div>${d.checkedInAt?`<div><span>Check-in</span><b>${esc(d.checkedInAt)}</b></div>`:''}</div>`:''}`;
   if(ok)setTimeout(()=>location.reload(),2200);
  }catch(e){result.className='scan-result danger';result.innerHTML='<div class="scan-icon">!</div><h3>Không kết nối được máy chủ</h3><p>Hãy thử lại hoặc nhập mã booking thủ công.</p>'}
  finally{busy=false}
 }
 manualBtn?.addEventListener('click',()=>submit(manual.value));
 manual?.addEventListener('keydown',e=>{if(e.key==='Enter')submit(manual.value)});
 if(window.Html5QrcodeScanner){
  const scanner=new Html5QrcodeScanner('reader',{fps:10,qrbox:{width:240,height:240},rememberLastUsedCamera:true},false);
  scanner.render(text=>submit(text),()=>{});
 }else{
  document.getElementById('reader').innerHTML='<div class="alert alert-warning">Không tải được thư viện camera QR. Bạn vẫn có thể nhập mã booking thủ công.</div>';
 }
})();
