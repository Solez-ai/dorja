'use client';
import { useState, useEffect, useRef } from 'react';
import { Plus, X, Camera, Trash2, MapPin, CheckCircle } from 'lucide-react';
const API = 'http://localhost:4000';
const RT = ['LIVING_ROOM','BEDROOM','KITCHEN','BATHROOM','BALCONY','DINING_ROOM'];
const RL: Record<string,string> = {LIVING_ROOM:'Living Room',BEDROOM:'Bedroom',KITCHEN:'Kitchen',BATHROOM:'Bathroom',BALCONY:'Balcony',DINING_ROOM:'Dining Room'};
type Photo = {id:string;url:string;filename:string;label:string;roomType:string};
type L = {id:string;slug:string;title:string;intent:string;propertyType:string;publicArea:string;priceAmount:number;status:string;rooms:{id:string;roomType:string;displayName:string}[]};
export default function MyListingsPage() {
  const [ls,setLs] = useState<L[]>([]);
  const [tok,setTok] = useState('');
  const [f,setF] = useState({title:'',intent:'RENT',propertyType:'APARTMENT',publicArea:'',exactAddress:'',price:''});
  const [rooms,setRooms] = useState<{type:string;label:string}[]>([]);
  const [photos,setPhotos] = useState<Photo[]>([]);
  const [ld,setLd] = useState(false);
  const [msg,setMsg] = useState('');
  const [show,setShow] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading,setUploading] = useState(false);
  useEffect(()=>{(async()=>{try{
    await fetch(API+'/v1/auth/otp/start',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:'+8801700000001'})});
    const vr=await fetch(API+'/v1/auth/otp/verify',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:'+8801700000001',code:'123456'})});
    const vd=await vr.json();const t=vd.data?.accessToken;
    if(t){setTok(t);const lr=await fetch(API+'/v1/listings?page=1&limit=50',{headers:{Authorization:'Bearer '+t}});const ld=await lr.json();if(ld.data)setLs(ld.data);}
  }catch{}})()},[]);
  const addRoom=()=>setRooms([...rooms,{type:RT[rooms.length%RT.length],label:''}]);
  const removeRoom=(i:number)=>setRooms(rooms.filter((_,idx)=>idx!==i));
  const handleUpload=async(e:React.ChangeEvent<HTMLInputElement>)=>{
    const files=e.target.files;if(!files||files.length===0)return;setUploading(true);
    for(const file of Array.from(files)){
      const fd=new FormData();fd.append('file',file);
      try{const r=await fetch(API+'/v1/photos/upload',{method:'POST',headers:{Authorization:'Bearer '+tok},body:fd});
      const d=await r.json();if(d.data)setPhotos(prev=>[...prev,{id:d.data.id,url:d.data.url,filename:file.name,label:'',roomType:RT[prev.length%RT.length]}]);}catch{}}
    setUploading(false);if(fileRef.current)fileRef.current.value='';
  };
  const updatePhotoLabel=(id:string,label:string)=>setPhotos(prev=>prev.map(p=>p.id===id?{...p,label}:p));
  const updatePhotoRoom=(id:string,rt:string)=>setPhotos(prev=>prev.map(p=>p.id===id?{...p,roomType:rt}:p));
  const removePhoto=(id:string)=>setPhotos(prev=>prev.filter(p=>p.id!==id));
  const submit=async()=>{
    if(!f.title||!f.publicArea||!f.exactAddress||!f.price||rooms.length===0){setMsg('Fill all fields and add rooms.');return;}
    setLd(true);setMsg('');
    try{const lr=await fetch(API+'/v1/listings',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+tok},
    body:JSON.stringify({title:f.title,intent:f.intent,propertyType:f.propertyType,publicArea:f.publicArea,exactAddress:f.exactAddress,priceAmount:parseInt(f.price,10),currency:'BDT',rooms:rooms.map((r,i)=>({roomType:r.type,displayName:r.label||RL[r.type],ordinal:i}))})});
    const ld=await lr.json();if(lr.ok&&ld.data){setMsg('Created! Your property is live.');setLs(p=>[{...ld.data,rooms:ld.data.rooms||[]},...p]);setF({title:'',intent:'RENT',propertyType:'APARTMENT',publicArea:'',exactAddress:'',price:''});setRooms([]);setPhotos([]);setShow(false);}
    else setMsg(ld.error?.message||'Failed.');}catch(e:any){setMsg('Error: '+e.message);}finally{setLd(false);}
  };
  const inp:React.CSSProperties={width:'100%',padding:'10px 12px',border:'1px solid var(--sand-300)',borderRadius:2,fontSize:14,fontFamily:'var(--font-body)'};
  const lbl:React.CSSProperties={fontSize:13,fontWeight:600,color:'var(--ink-800)',display:'block',marginBottom:4};
  return(<div style={{padding:'32px 40px',maxWidth:900}}>
    <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:24}}>
      <div><h1 style={{fontFamily:'var(--font-display)',marginBottom:4}}>My Listings</h1>
      <p style={{color:'var(--ink-800)',fontSize:14}}>Your properties on DORJA. Upload photos with custom room labels.</p></div>
      <button onClick={()=>setShow(!show)} className={show?'btn-outline':'btn-primary'}>{show?'Cancel':'+ Add Property'}</button>
    </div>
    {msg&&<div style={{padding:'12px 16px',background:msg.includes('Created')?'var(--leaf-100)':'var(--red-100)',borderRadius:2,marginBottom:16,fontSize:14,color:msg.includes('Created')?'var(--leaf-600)':'var(--red-600)'}}>{msg}</div>}
    {show&&(<div style={{background:'white',border:'1px solid var(--sand-300)',borderRadius:8,padding:24,marginBottom:24}}>
      <h3 style={{marginBottom:16,fontFamily:'var(--font-display)'}}>Add New Property</h3>
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginBottom:16}}>
        <div style={{gridColumn:'1/-1'}}><label style={lbl}>Title</label><input style={inp} value={f.title} onChange={e=>setF({...f,title:e.target.value})} placeholder='Family Apartment in Dhanmondi'/></div>
        <div><label style={lbl}>Intent</label><select style={inp} value={f.intent} onChange={e=>setF({...f,intent:e.target.value})}><option value='RENT'>For Rent</option><option value='SALE'>For Sale</option></select></div>
        <div><label style={lbl}>Type</label><select style={inp} value={f.propertyType} onChange={e=>setF({...f,propertyType:e.target.value})}>{['APARTMENT','HOUSE','ROOM','OFFICE','SHOP'].map(t=><option key={t} value={t}>{t.charAt(0)+t.slice(1).toLowerCase()}</option>)}</select></div>
        <div><label style={lbl}>Area</label><input style={inp} value={f.publicArea} onChange={e=>setF({...f,publicArea:e.target.value})} placeholder='Dhanmondi 27'/></div>
        <div><label style={lbl}>Price (BDT)</label><input style={inp} value={f.price} onChange={e=>setF({...f,price:e.target.value})} placeholder='25000' type='number'/></div>
        <div style={{gridColumn:'1/-1'}}><label style={lbl}>Exact Address (private)</label><input style={inp} value={f.exactAddress} onChange={e=>setF({...f,exactAddress:e.target.value})} placeholder='Full address for SafeView'/></div>
      </div>
      <div style={{marginBottom:16}}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
          <label style={{...lbl,marginBottom:0}}>Rooms & Custom Labels</label>
          <button onClick={addRoom} className='btn-outline' style={{padding:'4px 10px',fontSize:12}}><Plus size={12}/> Add Room</button>
        </div>
        {rooms.length===0&&<p style={{color:'var(--sand-400)',fontSize:13}}>Click + to add rooms with custom labels</p>}
        {rooms.map((r,i)=>(<div key={i} style={{display:'flex',gap:8,marginBottom:6,alignItems:'center'}}>
          <select value={r.type} onChange={e=>{const n=[...rooms];n[i].type=e.target.value;setRooms(n);}} style={{...inp,width:140}}>{RT.map(t=><option key={t} value={t}>{RL[t]}</option>)}</select>
          <input style={{...inp,flex:1}} value={r.label} onChange={e=>{const n=[...rooms];n[i].label=e.target.value;setRooms(n);}} placeholder='Custom label (e.g. Master Bedroom with AC)'/>
          <button onClick={()=>removeRoom(i)} style={{background:'none',border:'none',color:'var(--red-600)',cursor:'pointer'}}><Trash2 size={16}/></button>
        </div>))}
      </div>
      <div style={{marginBottom:16}}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
          <label style={{...lbl,marginBottom:0}}>Room Photos ({photos.length})</label>
          <button onClick={()=>fileRef.current?.click()} disabled={uploading} className='btn-outline' style={{padding:'4px 10px',fontSize:12}}><Camera size={12}/> {uploading?'Uploading...':'Add Photos'}</button>
          <input ref={fileRef} type='file' accept='image/*' multiple onChange={handleUpload} style={{display:'none'}}/>
        </div>
        <p style={{fontSize:12,color:'var(--sand-400)',marginBottom:8}}>Upload photos of each room and label them. This makes your listing look like a professional captured tour.</p>
        {photos.length>0&&(<div className='photo-grid'>
          {photos.map(p=>(<div key={p.id} className='photo-card'>
            <div style={{position:'relative'}}>
              <img src={p.url} alt={p.filename}/>
              <button onClick={()=>removePhoto(p.id)} style={{position:'absolute',top:4,right:4,background:'rgba(0,0,0,0.5)',border:'none',borderRadius:'50%',width:24,height:24,display:'flex',alignItems:'center',justifyContent:'center',cursor:'pointer',color:'white'}}><X size={14}/></button>
            </div>
            <div className='photo-card-body'>
              <input className='form-input' style={{padding:'6px 8px',fontSize:12,marginBottom:4}} value={p.label} onChange={e=>updatePhotoLabel(p.id,e.target.value)} placeholder='Room name'/>
              <select value={p.roomType} onChange={e=>updatePhotoRoom(p.id,e.target.value)} className='form-select' style={{padding:'6px 8px',fontSize:12}}>{RT.map(t=><option key={t} value={t}>{RL[t]}</option>)}</select>
            </div>
          </div>))}
        </div>)}
      </div>
      <button onClick={submit} disabled={ld} className='btn-primary' style={{width:'100%'}}>{ld?'Creating...':'Create Listing with Photos'}</button>
    </div>)}
    <h3 style={{marginBottom:12,fontFamily:'var(--font-display)'}}>All Listings ({ls.length})</h3>
    {ls.length===0?<p style={{color:'var(--sand-400)'}}>No listings yet. Click + Add Property to create one.</p>:<div style={{display:'flex',flexDirection:'column',gap:8}}>{ls.map(l=><a key={l.id} href={'/properties/'+l.slug} className='listing-card' style={{display:'flex',justifyContent:'space-between',alignItems:'center',padding:'14px 18px',textDecoration:'none',color:'inherit'}}>
      <div><div style={{fontWeight:600,fontSize:15,marginBottom:2}}>{l.title}</div>
      <div style={{fontSize:13,color:'var(--ink-800)',display:'flex',alignItems:'center',gap:6}}><MapPin size={12}/>{l.publicArea} · {l.propertyType.replace(/_/g,' ')} · {l.rooms?.length||0} rooms</div></div>
      <div style={{textAlign:'right'}}><div style={{fontFamily:'var(--font-mono)',fontWeight:700,fontSize:16}}>BDT {l.priceAmount.toLocaleString()}</div>
      <div style={{fontSize:11,fontFamily:'var(--font-mono)',color:l.status==='ACTIVE'?'var(--jol-700)':'var(--sand-400)',fontWeight:600,display:'flex',alignItems:'center',gap:4}}><CheckCircle size={10}/> {l.status}</div></div></a>)}</div>}
  </div>);
}
